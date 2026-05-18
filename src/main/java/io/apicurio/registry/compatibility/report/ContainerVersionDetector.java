package io.apicurio.registry.compatibility.report;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects the actual container image tag and digest for running containers
 * by querying the container runtime (podman or docker).
 *
 * Queries two fields from container inspection:
 * - {@code .Image} — the image ID (SHA256 digest of the image layer)
 * - {@code .Config.Image} — the original image reference used at creation time
 *       (e.g., {@code quay.io/apicurio/apicurio-registry:latest-snapshot})
 */
public class ContainerVersionDetector {

    private static final Pattern IMAGE_REF_PATTERN = Pattern.compile(
            "^(?:([^/]+)/)?([^:@]+)(?::([^@]+))?(?:@(.+))?$");

    private static final String APICURIO_CONTAINER = "compat-apicurio-sr";
    private static final String CONFLUENT_CONTAINER = "compat-confluent-sr";

    /**
     * Detected image info for a single container.
     */
    public record ImageInfo(
            String registry,
            String repository,
            String tag,
            String digest,
            String fullImage,
            boolean detected
    ) {
        public String displayVersion() {
            if (tag != null && !tag.isEmpty()) {
                return tag;
            }
            if (digest != null && !digest.isEmpty()) {
                return digest.length() > 19
                        ? digest.substring(0, 19) + "..."
                        : digest;
            }
            return fullImage != null ? fullImage : "unknown";
        }

        public String shortDigest() {
            if (digest == null || digest.isEmpty()) return "";
            String d = digest.startsWith("sha256:") ? digest.substring("sha256:".length()) : digest;
            return d.length() > 12 ? d.substring(0, 12) : d;
        }

        public String registryUrl() {
            if (registry == null || repository == null) return "";
            String tagPart = tag != null ? ":" + tag : "";
            return "https://" + registry + "/" + repository + tagPart;
        }
    }

    /**
     * Detected versions for both registry containers.
     */
    public record ContainerVersions(
            ImageInfo apicurio,
            ImageInfo confluent
    ) {}

    public ContainerVersions detect() {
        String runtime = findRuntime();
        if (runtime == null) {
            return new ContainerVersions(
                    unknownImage(APICURIO_CONTAINER),
                    unknownImage(CONFLUENT_CONTAINER));
        }
        return new ContainerVersions(
                detectImage(runtime, APICURIO_CONTAINER),
                detectImage(runtime, CONFLUENT_CONTAINER));
    }

    ImageInfo detectImage(String containerName) {
        return detectImage(findRuntime(), containerName);
    }

    private ImageInfo detectImage(String runtime, String containerName) {
        String configImage = inspectField(runtime, containerName, "{{.Config.Image}}");
        String imageId = inspectField(runtime, containerName, "{{.Image}}");

        if (configImage == null || configImage.isEmpty()) {
            if (imageId != null && !imageId.isEmpty()) {
                ImageInfo info = parseImageRef(imageId);
                return new ImageInfo(info.registry(), info.repository(), info.tag(), info.digest(), info.fullImage(), true);
            }
            return unknownImage(containerName);
        }

        ImageInfo info = parseImageRef(configImage);
        if ((info.digest() == null || info.digest().isEmpty()) && imageId != null) {
            return new ImageInfo(info.registry(), info.repository(), info.tag(), imageId, info.fullImage(), true);
        }
        return new ImageInfo(info.registry(), info.repository(), info.tag(), info.digest(), info.fullImage(), true);
    }

    private String findRuntime() {
        for (String cmd : new String[]{"podman", "docker"}) {
            try {
                Process p = new ProcessBuilder(cmd, "version", "--format", "{{.Client.Version}}")
                        .redirectErrorStream(true).start();
                if (p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0) {
                    return cmd;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String inspectField(String runtime, String containerName, String format) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    runtime, "inspect", containerName,
                    "--format", format);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine();
            p.waitFor(5, TimeUnit.SECONDS);
            return line != null ? line.trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    ImageInfo parseImageRef(String imageRef) {
        String image = imageRef.trim();
        if (image.startsWith("sha256:")) {
            return new ImageInfo(null, null, null, image, imageRef, true);
        }

        Matcher m = IMAGE_REF_PATTERN.matcher(image);
        if (m.matches()) {
            return new ImageInfo(m.group(1), m.group(2), m.group(3), m.group(4), imageRef, true);
        }

        return new ImageInfo(null, null, null, null, imageRef, true);
    }

    private ImageInfo unknownImage(String containerName) {
        return new ImageInfo(null, null, null, null, containerName + " (version undetected)", false);
    }
}
