package io.apicurio.registry.compatibility.staticanalysis;

import static io.restassured.RestAssured.given;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.apicurio.registry.compatibility.config.TestConfiguration;
import io.apicurio.registry.compatibility.shared.AbstractCompatibilityTest;
import io.apicurio.registry.compatibility.shared.CompatibilityReportExtension;
import io.restassured.response.Response;

@DisplayName("Static OpenAPI Spec Example Validation")
@ExtendWith(CompatibilityReportExtension.class)
class OpenApiExampleValidationTest extends AbstractCompatibilityTest {

    private static final String SIMPLE_AVRO_SCHEMA = "{\"type\":\"string\"}";

    private String testSubject;

    @BeforeEach
    void setupTestSubject() {
        testSubject = subjectName("spec-example");

        String body = "{\"schema\": " + escapeJson(SIMPLE_AVRO_SCHEMA) + "}";

        given()
                .contentType(TestConfiguration.SCHEMA_REGISTRY_CONTENT_TYPE)
                .body(body)
                .post(confluentUrl() + "/subjects/{subject}/versions", testSubject);

        given()
                .contentType(TestConfiguration.SCHEMA_REGISTRY_CONTENT_TYPE)
                .body(body)
                .post(apicurioUrl() + "/subjects/{subject}/versions", testSubject);
    }

    @Test
    @DisplayName("Sends spec-derived example payloads to both registries")
    void validateExamplePayloads() {
        OpenApiSpecProvider provider = new OpenApiSpecProvider();
        var spec = provider.loadConfluentSpec();

        ExamplePayloadExtractor extractor = new ExamplePayloadExtractor(spec);
        List<ExamplePayloadExtractor.ExampleRequest> examples = extractor.extract();

        System.out.println("[spec-example] Extracted " + examples.size()
                + " example requests from spec.");

        for (ExamplePayloadExtractor.ExampleRequest example : examples) {
            String resolvedPath = example.path()
                    .replace("{subject}", testSubject)
                    .replace("{version}", "latest");

            Response confluentResp = given()
                    .contentType(example.contentType())
                    .body(example.payload())
                    .request(example.method(), confluentUrl() + resolvedPath);

            Response apicurioResp = given()
                    .contentType(example.contentType())
                    .body(example.payload())
                    .request(example.method(), apicurioUrl() + resolvedPath);

            assertCompatibility(
                    example.testName(),
                    example.method(),
                    resolvedPath,
                    confluentResp, apicurioResp,
                    false,
                    example.payload());
        }
    }
}
