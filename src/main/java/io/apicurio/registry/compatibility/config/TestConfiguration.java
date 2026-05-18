package io.apicurio.registry.compatibility.config;

/**
 * Configuration for the compatibility test harness.
 * URLs are loaded from system properties or defaults to localhost containers.
 */
public class TestConfiguration {

    public static final String SCHEMA_REGISTRY_CONTENT_TYPE = "application/vnd.schemaregistry.v1+json";
    public static final String JSON_CONTENT_TYPE = "application/json";

    private static final String CONFLUENT_URL_PROPERTY = "confluent.registry.url";
    private static final String APICURIO_URL_PROPERTY = "apicurio.registry.url";
    private static final String API_LEVEL_PROPERTY = "api.level";

    private static final String DEFAULT_CONFLUENT_URL = "http://localhost:8081";
    private static final String DEFAULT_API_LEVEL = "v8";

    public String getConfluentRegistryUrl() {
        return System.getProperty(CONFLUENT_URL_PROPERTY, DEFAULT_CONFLUENT_URL);
    }

    public String getApicurioRegistryUrl() {
        return System.getProperty(APICURIO_URL_PROPERTY,
                "http://localhost:8082/apis/ccompat/" + getApiLevel());
    }

    public static String getApiLevel() {
        return System.getProperty(API_LEVEL_PROPERTY, DEFAULT_API_LEVEL);
    }
}
