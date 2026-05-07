#!/usr/bin/env bash
# Checks Docker Hub for the latest cp-schema-registry tag and compares
# it to the version pinned in podman-compose.yml.
# Outputs to GITHUB_OUTPUT for workflow interop.

set -euo pipefail

COMPOSE_FILE="${1:-podman-compose.yml}"

# Extract the currently pinned version
PINNED=$(grep -oP 'cp-schema-registry:\K[0-9]+\.[0-9]+\.[0-9]+' "$COMPOSE_FILE")
if [ -z "$PINNED" ]; then
  echo "ERROR: Could not extract Confluent version from $COMPOSE_FILE" >&2
  exit 2
fi

echo "Pinned version: $PINNED"

# Query Docker Hub for available tags
TAGS_JSON=$(curl -sf "https://hub.docker.com/v2/repositories/confluentinc/cp-schema-registry/tags?page_size=100")

if [ -z "$TAGS_JSON" ]; then
  echo "ERROR: Could not fetch tags from Docker Hub" >&2
  exit 2
fi

# Extract numeric semver tags, sort, pick latest
LATEST=$(echo "$TAGS_JSON" \
  | grep -oP '"name"\s*:\s*"\K[0-9]+\.[0-9]+\.[0-9]+' \
  | sort -V \
  | tail -1)

if [ -z "$LATEST" ]; then
  echo "ERROR: Could not parse version tags from Docker Hub" >&2
  exit 2
fi

echo "Latest on Docker Hub: $LATEST"

# Semver comparison
L_MAJ=$(echo "$LATEST" | cut -d. -f1)
L_MIN=$(echo "$LATEST" | cut -d. -f2)
L_PAT=$(echo "$LATEST" | cut -d. -f3)
P_MAJ=$(echo "$PINNED" | cut -d. -f1)
P_MIN=$(echo "$PINNED" | cut -d. -f2)
P_PAT=$(echo "$PINNED" | cut -d. -f3)

NEWER=0
if [ "$L_MAJ" -gt "$P_MAJ" ]; then
  NEWER=1
elif [ "$L_MAJ" -eq "$P_MAJ" ] && [ "$L_MIN" -gt "$P_MIN" ]; then
  NEWER=1
elif [ "$L_MAJ" -eq "$P_MAJ" ] && [ "$L_MIN" -eq "$P_MIN" ] && [ "$L_PAT" -gt "$P_PAT" ]; then
  NEWER=1
fi

if [ "$NEWER" -eq 1 ]; then
  echo "NEW_VERSION=$LATEST" >> "${GITHUB_OUTPUT:-/dev/null}"
  echo "PINNED_VERSION=$PINNED" >> "${GITHUB_OUTPUT:-/dev/null}"
  echo "UPDATE_NEEDED=true" >> "${GITHUB_OUTPUT:-/dev/null}"
  echo "Newer version available: $LATEST (currently testing $PINNED)"
else
  echo "UPDATE_NEEDED=false" >> "${GITHUB_OUTPUT:-/dev/null}"
  echo "Confluent Schema Registry is up to date ($PINNED)"
fi
