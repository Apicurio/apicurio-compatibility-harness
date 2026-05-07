package io.apicurio.registry.compatibility.staticanalysis;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;

/**
 * Loads OpenAPI specs from classpath resources or system property override URLs.
 */
public class OpenApiSpecProvider {

    private static final String CONFLUENT_RESOURCE = "openapi/confluent-sr-v8.yaml";
    private static final String APICURIO_RESOURCE = "openapi/apicurio-ccompat-v8.yaml";

    private static final String PROP_CONFLUENT_URL = "static.spec.confluent.url";
    private static final String PROP_APICURIO_URL = "static.spec.apicurio.url";

    private final OpenAPIV3Parser parser = new OpenAPIV3Parser();
    private final ParseOptions options = new ParseOptions();

    {
        options.setResolve(true);
        options.setFlatten(true);
    }

    public OpenAPI loadConfluentSpec() {
        String url = System.getProperty(PROP_CONFLUENT_URL);
        if (url != null && !url.isBlank()) {
            return parse(url, "Confluent (from system property)");
        }
        return parseResource(CONFLUENT_RESOURCE, "Confluent");
    }

    public OpenAPI loadApicurioSpec() {
        String url = System.getProperty(PROP_APICURIO_URL);
        if (url != null && !url.isBlank()) {
            return parse(url, "Apicurio (from system property)");
        }
        return parseResource(APICURIO_RESOURCE, "Apicurio");
    }

    /**
     * Writes the Confluent spec to a temp file and returns its path.
     * Needed because openapi-diff accepts file paths or URLs.
     */
    public Path writeConfluentSpecToTemp() {
        return writeResourceToTemp(CONFLUENT_RESOURCE);
    }

    /**
     * Writes the Apicurio spec to a temp file, if available on classpath.
     * Throws IllegalStateException if not found.
     */
    public Path writeApicurioSpecToTemp() {
        return writeResourceToTemp(APICURIO_RESOURCE);
    }

    private OpenAPI parse(String location, String label) {
        OpenAPI result = parser.read(location, null, options);
        if (result == null) {
            throw new IllegalStateException("Failed to parse " + label + " OpenAPI spec from: " + location);
        }
        return result;
    }

    private OpenAPI parseResource(String resourcePath, String label) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalStateException(label + " OpenAPI spec not found on classpath: " + resourcePath);
        }
        try (is) {
            String content = new String(is.readAllBytes());
            OpenAPI result = parser.readContents(content, null, options).getOpenAPI();
            if (result == null) {
                throw new IllegalStateException("Failed to parse " + label + " OpenAPI spec from classpath");
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Error reading " + label + " OpenAPI spec", e);
        }
    }

    private Path writeResourceToTemp(String resourcePath) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalStateException("Resource not found: " + resourcePath);
            }
            Path temp = Files.createTempFile("openapi-spec-", ".yaml");
            Files.write(temp, is.readAllBytes());
            temp.toFile().deleteOnExit();
            return temp;
        } catch (Exception e) {
            throw new RuntimeException("Error writing spec to temp file", e);
        }
    }
}
