package io.apicurio.registry.compatibility.staticanalysis;

import java.util.ArrayList;
import java.util.List;

import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;
import org.openapitools.openapidiff.core.model.ChangedOperation;
import org.openapitools.openapidiff.core.model.ChangedParameters;
import org.openapitools.openapidiff.core.model.DiffResult;
import org.openapitools.openapidiff.core.model.Endpoint;

import io.swagger.v3.oas.models.parameters.Parameter;

import io.apicurio.registry.compatibility.model.CompatibilityTestResult;
import io.apicurio.registry.compatibility.model.TestOutcome;

/**
 * Compares two OpenAPI specs and produces TestOutcome records for each difference found.
 */
public class OpenApiDiffAnalyzer {

    private final String confluentSpecPath;
    private final String apicurioSpecPath;

    public OpenApiDiffAnalyzer(String confluentSpecPath, String apicurioSpecPath) {
        this.confluentSpecPath = confluentSpecPath;
        this.apicurioSpecPath = apicurioSpecPath;
    }

    public List<TestOutcome> analyze() {
        ChangedOpenApi diff = OpenApiCompare.fromLocations(confluentSpecPath, apicurioSpecPath);
        List<TestOutcome> outcomes = new ArrayList<>();

        // Endpoints in Confluent but missing from Apicurio
        for (Endpoint missing : diff.getMissingEndpoints()) {
            String method = missing.getMethod().name();
            outcomes.add(buildOutcome(
                    "Missing endpoint " + method + " " + missing.getPathUrl(),
                    CompatibilityTestResult.SKIPPED_GAP,
                    "DEFINED", "MISSING",
                    "Confluent defines " + method + " " + missing.getPathUrl()
                            + " but Apicurio does not expose it."));
        }

        // Endpoints in Apicurio but not in Confluent (informational)
        for (Endpoint extra : diff.getNewEndpoints()) {
            String method = extra.getMethod().name();
            outcomes.add(buildOutcome(
                    "Extra endpoint " + method + " " + extra.getPathUrl(),
                    CompatibilityTestResult.PASS,
                    "N/A", "EXTRA",
                    "Apicurio exposes " + method + " " + extra.getPathUrl()
                            + " which is not in the Confluent spec."));
        }

        // Changed operations
        for (ChangedOperation changed : diff.getChangedOperations()) {
            outcomes.addAll(analyzeChangedOperation(changed));
        }

        return outcomes;
    }

    private List<TestOutcome> analyzeChangedOperation(ChangedOperation changed) {
        List<TestOutcome> outcomes = new ArrayList<>();
        String method = changed.getHttpMethod().name();
        String path = changed.getPathUrl();
        String baseName = method + " " + path;

        // Parameter differences
        ChangedParameters params = changed.getParameters();
        if (params != null && params.isCoreChanged() != DiffResult.NO_CHANGES) {
            for (Parameter p : params.getMissing()) {
                boolean required = p.getRequired() != null && p.getRequired();
                outcomes.add(buildOutcome(
                        "Missing param '" + p.getName() + "' on " + baseName,
                        required ? CompatibilityTestResult.FAIL : CompatibilityTestResult.SKIPPED_GAP,
                        "DEFINED", "MISSING",
                        "Confluent defines " + (required ? "required" : "optional")
                                + " parameter '" + p.getName() + "' (" + p.getIn()
                                + ") which Apicurio does not accept."));
            }
        }

        // Request body differences
        if (changed.getRequestBody() != null
                && changed.getRequestBody().isCoreChanged() != DiffResult.NO_CHANGES) {
            outcomes.add(buildOutcome(
                    "Request body diff on " + baseName,
                    CompatibilityTestResult.FAIL,
                    "DIFFERENT", "DIFFERENT",
                    "Request body schema differs for " + baseName + "."));
        }

        // Response differences
        if (changed.getApiResponses() != null
                && changed.getApiResponses().isCoreChanged() != DiffResult.NO_CHANGES) {
            outcomes.add(buildOutcome(
                    "Response diff on " + baseName,
                    CompatibilityTestResult.FAIL,
                    "DIFFERENT", "DIFFERENT",
                    "Response schema differs for " + baseName + "."));
        }

        // If no specific sub-changes were found, record a generic change
        if (outcomes.isEmpty()) {
            outcomes.add(buildOutcome(
                    "Operation changed: " + baseName,
                    CompatibilityTestResult.FAIL,
                    "CHANGED", "CHANGED",
                    "API operation " + baseName
                            + " has structural differences between Confluent and Apicurio."));
        }

        return outcomes;
    }

    private TestOutcome buildOutcome(String testName, CompatibilityTestResult result,
            String confluentStatus, String apicurioStatus, String details) {
        return TestOutcome.builder()
                .testName("static-diff: " + testName)
                .method("DIFF")
                .endpoint("openapi-spec")
                .confluentStatus(confluentStatus)
                .apicurioStatus(apicurioStatus)
                .result(result)
                .details(details)
                .build();
    }
}
