
<!-- BACKLOG.MD MCP GUIDELINES START -->

<CRITICAL_INSTRUCTION>

## BACKLOG WORKFLOW INSTRUCTIONS

This project uses Backlog.md MCP for all task and project management activities.

**CRITICAL GUIDANCE**

- If your client supports MCP resources, read `backlog://workflow/overview` to understand when and how to use Backlog for this project.
- If your client only supports tools or the above request fails, call `backlog.get_backlog_instructions()` to load the tool-oriented overview. Use the `instruction` selector when you need `task-creation`, `task-execution`, or `task-finalization`.

- **First time working here?** Read the overview resource IMMEDIATELY to learn the workflow
- **Already familiar?** You should have the overview cached ("## Backlog.md Overview (MCP)")
- **When to read it**: BEFORE creating tasks, or when you're unsure whether to track work

These guides cover:
- Decision framework for when to create tasks
- Search-first workflow to avoid duplicates
- Links to detailed guides for task creation, execution, and finalization
- MCP tools reference

You MUST read the overview resource to understand the complete workflow. The information is NOT summarized here.

</CRITICAL_INSTRUCTION>

<!-- BACKLOG.MD MCP GUIDELINES END -->

---

# Apicurio Registry - Confluent v8 API Compatibility Harness

## Project Goal

Build an automated, reproducible test harness that compares Apicurio Registry's Confluent-compatible API against the official Confluent Schema Registry API v8. The output is a standalone HTML compatibility report. This project will be contributed back to the Apicurio organization, so all code must match their conventions.

**PRD Location**: `PRD.md` (authoritative requirements source)

## Upstream Reference: Apicurio Registry

- **Repository**: https://github.com/Apicurio/apicurio-registry
- **Current version**: `3.3.0-SNAPSHOT`
- **Java version**: 17 (`maven.compiler.release=17`)
- **Framework**: Quarkus **3.33.1**
- **Build tool**: Maven
- **GroupId**: `io.apicurio`
- **Confluent client version**: `8.0.0` (already declared in root POM as `confluent.version`)
- **Avro version**: `1.12.1`

### Key Apicurio Conventions

- **POM structure**: Root POM defines `quarkus.version` and all dependency versions as properties. Sub-modules inherit from root.
- **BOM import**: `io.quarkus:quarkus-bom` imported in `<dependencyManagement>`
- **Module naming**: kebab-case directory names (e.g., `app`, `common`, `serdes`, `schema-util`, `integration-tests`)
- **Artifact naming**: `apicurio-registry-{module}` pattern
- **Package naming**: `io.apicurio.registry.*`
- **Test naming**: `*Test.java` for unit tests, `*IT.java` for integration tests
- **Test framework**: JUnit 5, REST Assured (static imports from `io.restassured.RestAssured.given`)
- **Container testing**: `@QuarkusTestResource` with `QuarkusTestResourceLifecycleManager`
- **CDI annotations**: `@ApplicationScoped`, `@Inject`, `@Interceptors` (no Lombok)
- **Logging**: SLF4J (`org.slf4j.Logger`) with `@Logged` interceptor
- **Java version**: 17, 4-space indent, ~120 char line length
- **Contribution**: DCO sign-off required, squash-and-merge PRs, tests + docs mandatory

### Apicurio Confluent-Compatible API Module

Apicurio exposes Confluent-compatible REST endpoints in the `app/` module at:
- **v7**: `/apis/ccompat/v7/` (fully supported)
- **v8**: `/apis/ccompat/v8/` (fully supported)

Source code at: `app/src/main/java/io/apicurio/registry/ccompat/rest/`
- `v7/` - v7 API interfaces and implementations
- `v8/` - v8 API interfaces (delegate to v7 impls), includes:
  - `SubjectsResourceImpl.java` - delegates to v7 `SubjectsResourceImpl`
  - `SchemasResourceImpl.java`
  - `CompatibilityResourceImpl.java`
  - `ConfigResourceImpl.java`
  - `ModeResourceImpl.java`
  - `ContextResourceImpl.java`
  - `ExporterResourceImpl.java`
- `error/` - Error handling (ConflictException, SchemaNotFoundException, etc.)

### Feature Support (from ccompat README)

**Fully Supported**: Schemas, Subjects, Compatibility, Config, Mode, Contexts
**Partially Supported**: Data Contracts (metadata/ruleSet passthrough, NOT enforced)
**Not Supported**: Schema Linking/Exporters, KEKs/DEKs, Cluster Metadata

## Technology Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 17+ |
| Framework | Quarkus (Test) |
| Build | Maven |
| API Testing | REST Assured |
| Containers | podman + podman-compose |
| Static Analysis | openapi-diff |
| Reporting | Qute (Quarkus templating) or custom HTML |
| CI | GitHub Actions |

## Confluent Schema Registry API v8

**Note**: "v8" refers to the Confluent Platform 7.x series (CP 7.x = Schema Registry API v8).

### Complete Endpoint Map

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/subjects` | List all subjects |
| GET | `/subjects/{subject}/versions` | List versions under a subject |
| GET | `/subjects/{subject}/versions/{version}` | Get schema by version |
| GET | `/subjects/{subject}/versions/{version}/schema` | Get raw schema string |
| POST | `/subjects/{subject}/versions` | Register schema under subject |
| POST | `/subjects/{subject}/versions/{version}` | Look up schema version |
| DELETE | `/subjects/{subject}/versions/{version}` | Delete version (soft) |
| DELETE | `/subjects/{subject}/versions/{version}?permanent=true` | Delete version (permanent) |
| DELETE | `/subjects/{subject}` | Delete subject entirely |
| GET | `/subjects/{subject}/versions/{version}/referencedby` | Find referents |
| GET | `/schemas/ids/{id}` | Get schema by global ID |
| GET | `/schemas/ids/{id}/versions` | Get all versions for a schema ID |
| GET | `/schemas/ids/{id}/subjects` | Get all subjects referencing a schema ID |
| POST | `/compatibility/subjects/{subject}/versions/{version}` | Test compatibility |
| GET | `/config` | Get global compatibility config |
| PUT | `/config` | Set global compatibility config |
| GET | `/config/{subject}` | Get compatibility config for subject |
| PUT | `/config/{subject}` | Set compatibility config for subject |
| GET | `/mode` | Get registry mode |
| PUT | `/mode` | Set registry mode |

### Confluent Error Codes

| Code | Name | HTTP | Meaning |
|------|------|------|---------|
| `40401` | SUBJECT_NOT_FOUND | 404 | Subject does not exist |
| `40402` | VERSION_NOT_FOUND | 404 | Version does not exist under subject |
| `40403` | SCHEMA_NOT_FOUND | 404 | Schema with given global ID not found |
| `40901` | INCOMPATIBLE_SCHEMA | 409 | Schema incompatible with previous versions |
| `42201` | INVALID_SCHEMA | 422 | Schema is invalid/malformed |
| `42202` | INVALID_VERSION | 422 | Invalid version identifier |
| `42203` | INVALID_COMPATIBILITY | 422 | Invalid compatibility level |
| `50001` | INTERNAL_SERVER_ERROR | 500 | Unexpected internal error |
| `50003` | KAFKA_ERROR | 500 | Error communicating with Kafka |

### Mode Values

READWRITE (default), READONLY, READONLY_OVERRIDE, IMPORT, MIGRATE

### Request Content-Type

`application/vnd.schemaregistry.v1+json` (standard) or `application/json`

### Avro Schema Canonicalization

Avro schemas are normalized using `SchemaNormalization.toParsingForm()` before comparison. Whitespace, doc fields, and aliases are ignored. Two logically identical schemas with different formatting get the same ID.

### Compatibility Levels

NONE, BACKWARD, BACKWARD_TRANSITIVE, FORWARD, FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE

### Scope Restriction (v1.0)

- **Avro only** - Protobuf and JSON Schema are out of scope
- All Confluent v8 API endpoints must be covered
- Missing features in Apicurio must be flagged as `SKIPPED_GAP` / `KNOWN_INCOMPATIBILITY`, not hard failures

## Architecture

### Module Structure (Target)

```
apicurio-compatibility-harness/
  pom.xml                          # Root POM, aligned with Apicurio conventions
  podman-compose.yml               # Confluent + Apicurio + Kafka containers
  src/
    main/
      java/io/apicurio/registry/compatibility/
        config/                    # Test configuration, base URLs
        model/                     # Shared DTOs, error codes, test result models
        staticanalysis/            # OpenAPI diff, example validation
        report/                    # HTML report generation
    test/
      java/io/apicurio/registry/compatibility/
        fixtures/                  # Avro schema test data, example payloads
        shared/                    # REST Assured clients, response validators
        subjects/                  # /subjects endpoint tests
        schemas/                   # /schemas endpoint tests
        compatibility/             # /compatibility endpoint tests
        config/                    # /config endpoint tests
        mode/                      # /mode endpoint tests
```

### Container Services (podman-compose.yml)

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| kafka | `apache/kafka:3.9.0` | 9092 | Kafka broker (KRaft mode, no Zookeeper) |
| confluent-sr | `confluentinc/cp-schema-registry:7.8.0` | 8081 | Confluent Schema Registry v8 |
| apicurio-sr | `quay.io/apicurio/apicurio-registry-kafkasql:latest-snapshot` | 8082 | Apicurio Registry (nightly, shares Kafka) |

**Key**: Kafka uses KRaft mode (no Zookeeper needed). Both registries share the same Kafka cluster but use separate topics.

### Test Lifecycle

1. `podman-compose up -d` starts all services
2. Health checks confirm both registries are ready
3. Static analysis phase: OpenAPI diff + example validation
4. Runtime phase: REST Assured tests against both registries
5. Report phase: Generate HTML compatibility report
6. `podman-compose down` tears down services

## Key Design Decisions

### Test Result States

| State | Meaning |
|-------|---------|
| `PASS` | Apicurio matches Confluent behavior exactly |
| `FAIL` | Apicurio response differs from Confluent |
| `SKIPPED_GAP` | Apicurio does not support this feature (known gap) |
| `KNOWN_INCOMPATIBILITY` | Intentional deviation documented by Apicurio |
| `ERROR` | Test infrastructure issue (not an API gap) |

### Dual-Target Testing Pattern

Each test sends the **same request** to both:
- `http://localhost:8081` (Confluent Schema Registry)
- `http://localhost:8082/apis/ccompat/v8` (Apicurio Registry - note the `/apis/` prefix)

Then compares status codes, response bodies, and error codes.

## CI/CD (GitHub Actions)

Workflow: `.github/workflows/compatibility-tests.yml`
- Runs on push/PR
- Installs podman + podman-compose
- Bootstraps containers, waits for health checks
- Runs `mvn clean test`
- Uploads `target/compatibility-report.html` as artifact

## Current Status

- [ ] Project initialized with PRD
- [ ] Research phase (in progress - agents gathering Apicurio conventions)
- [ ] Backlog tasks to be created
- [ ] Implementation pending

## Commands

```bash
# Start test infrastructure
podman-compose up -d

# Run all tests and generate report
mvn clean test

# Run specific test class
mvn test -Dtest=SubjectsResourceTest

# Tear down infrastructure
podman-compose down
```
