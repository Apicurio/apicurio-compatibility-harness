package io.apicurio.registry.compatibility.model;

/**
 * Records the outcome of a single compatibility test comparison.
 */
public class TestOutcome {

    private final String testName;
    private final String endpoint;
    private final String method;
    private final CompatibilityTestResult result;
    private final String confluentStatus;
    private final String apicurioStatus;
    private final String details;
    private final String confluentBody;
    private final String apicurioBody;
    private final String testClassName;
    private final String testMethodName;
    private final String testSourceCode;
    private final String openApiOperation;
    private final String confluentDocUrl;
    private final String apicurioImplHint;

    private TestOutcome(Builder builder) {
        this.testName = builder.testName;
        this.endpoint = builder.endpoint;
        this.method = builder.method;
        this.result = builder.result;
        this.confluentStatus = builder.confluentStatus;
        this.apicurioStatus = builder.apicurioStatus;
        this.details = builder.details;
        this.confluentBody = builder.confluentBody;
        this.apicurioBody = builder.apicurioBody;
        this.testClassName = builder.testClassName;
        this.testMethodName = builder.testMethodName;
        this.testSourceCode = builder.testSourceCode;
        this.openApiOperation = builder.openApiOperation;
        this.confluentDocUrl = builder.confluentDocUrl;
        this.apicurioImplHint = builder.apicurioImplHint;
    }

    public String getTestName() { return testName; }
    public String getEndpoint() { return endpoint; }
    public String getMethod() { return method; }
    public CompatibilityTestResult getResult() { return result; }
    public String getConfluentStatus() { return confluentStatus; }
    public String getApicurioStatus() { return apicurioStatus; }
    public String getDetails() { return details; }
    public String getConfluentBody() { return confluentBody; }
    public String getApicurioBody() { return apicurioBody; }
    public String getTestClassName() { return testClassName; }
    public String getTestMethodName() { return testMethodName; }
    public String getTestSourceCode() { return testSourceCode; }
    public String getOpenApiOperation() { return openApiOperation; }
    public String getConfluentDocUrl() { return confluentDocUrl; }
    public String getApicurioImplHint() { return apicurioImplHint; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String testName;
        private String endpoint;
        private String method;
        private CompatibilityTestResult result;
        private String confluentStatus;
        private String apicurioStatus;
        private String details;
        private String confluentBody;
        private String apicurioBody;
        private String testClassName;
        private String testMethodName;
        private String testSourceCode;
        private String openApiOperation;
        private String confluentDocUrl;
        private String apicurioImplHint;

        public Builder testName(String testName) { this.testName = testName; return this; }
        public Builder endpoint(String endpoint) { this.endpoint = endpoint; return this; }
        public Builder method(String method) { this.method = method; return this; }
        public Builder result(CompatibilityTestResult result) { this.result = result; return this; }
        public Builder confluentStatus(String status) { this.confluentStatus = status; return this; }
        public Builder apicurioStatus(String status) { this.apicurioStatus = status; return this; }
        public Builder details(String details) { this.details = details; return this; }
        public Builder confluentBody(String body) { this.confluentBody = body; return this; }
        public Builder apicurioBody(String body) { this.apicurioBody = body; return this; }
        public Builder testClassName(String testClassName) { this.testClassName = testClassName; return this; }
        public Builder testMethodName(String testMethodName) { this.testMethodName = testMethodName; return this; }
        public Builder testSourceCode(String testSourceCode) { this.testSourceCode = testSourceCode; return this; }
        public Builder openApiOperation(String openApiOperation) { this.openApiOperation = openApiOperation; return this; }
        public Builder confluentDocUrl(String confluentDocUrl) { this.confluentDocUrl = confluentDocUrl; return this; }
        public Builder apicurioImplHint(String apicurioImplHint) { this.apicurioImplHint = apicurioImplHint; return this; }

        public TestOutcome build() { return new TestOutcome(this); }
    }
}
