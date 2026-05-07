package io.apicurio.registry.compatibility.staticanalysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;

/**
 * Extracts example request payloads from an OpenAPI spec's request body schemas.
 * Generates payloads using property-level examples, enum values, and type defaults.
 */
public class ExamplePayloadExtractor {

    public record ExampleRequest(
            String testName,
            String method,
            String path,
            String payload,
            String contentType) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String DEFAULT_AVRO_SCHEMA = "{\"type\":\"string\"}";

    private final OpenAPI spec;

    public ExamplePayloadExtractor(OpenAPI spec) {
        this.spec = spec;
    }

    public List<ExampleRequest> extract() {
        List<ExampleRequest> requests = new ArrayList<>();

        for (Map.Entry<String, PathItem> entry : spec.getPaths().entrySet()) {
            String path = entry.getKey();
            PathItem item = entry.getValue();

            addIfPresent(path, "POST", item.getPost(), requests);
            addIfPresent(path, "PUT", item.getPut(), requests);
        }

        return requests;
    }

    private void addIfPresent(String path, String method, Operation op,
            List<ExampleRequest> requests) {
        if (op == null || op.getRequestBody() == null || op.getRequestBody().getContent() == null) {
            return;
        }

        Content content = op.getRequestBody().getContent();

        String ct = "application/vnd.schemaregistry.v1+json";
        MediaType mediaType = content.get(ct);
        if (mediaType == null) {
            var first = content.entrySet().iterator().next();
            ct = first.getKey();
            mediaType = first.getValue();
        }

        if (mediaType == null || mediaType.getSchema() == null) {
            return;
        }

        String payload = generatePayload(mediaType.getSchema());
        String opId = op.getOperationId() != null
                ? op.getOperationId()
                : method + " " + path;

        requests.add(new ExampleRequest(
                "spec-example: " + opId,
                method, path, payload, ct));
    }

    @SuppressWarnings("unchecked")
    private String generatePayload(Schema<?> schema) {
        schema = resolveIfNeeded(schema);
        if (schema == null || schema.getProperties() == null) {
            return "{}";
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        for (Map.Entry<String, Schema> prop : schema.getProperties().entrySet()) {
            Object value = getValueForProperty(prop.getKey(), resolveIfNeeded(prop.getValue()));
            if (value != null) {
                payload.put(prop.getKey(), value);
            }
        }

        try {
            return MAPPER.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private Object getValueForProperty(String name, Schema<?> schema) {
        if (schema == null) {
            return null;
        }

        if ("schema".equals(name)) {
            return DEFAULT_AVRO_SCHEMA;
        }

        if (schema.getExample() != null) {
            return schema.getExample();
        }

        if (schema.getEnum() != null && !schema.getEnum().isEmpty()) {
            return schema.getEnum().get(0);
        }

        String type = schema.getType();
        if ("string".equals(type)) {
            return "";
        }
        if ("integer".equals(type)) {
            return 1;
        }
        if ("boolean".equals(type)) {
            return true;
        }

        return null;
    }

    private Schema<?> resolveIfNeeded(Schema<?> schema) {
        if (schema != null && schema.get$ref() != null) {
            String ref = schema.get$ref();
            String name = ref.substring(ref.lastIndexOf('/') + 1);
            if (spec.getComponents() != null && spec.getComponents().getSchemas() != null) {
                return spec.getComponents().getSchemas().get(name);
            }
        }
        return schema;
    }
}
