Here is the structured Product Requirements Document (PRD) tailored for your coding agents. It provides explicit instructions on architecture, frameworks, constraints, and execution steps.

***

# Product Requirements Document (PRD)
## Apicurio Registry - Confluent v8 API Compatibility Harness

### 1. Project Overview & Objectives
The goal of this project is to replace manual compatibility testing between Apicurio Registry and Confluent Schema Registry (API v8) with an automated, reproducible, and strictly structured test harness. This harness will perform both static compliance checks and dynamic runtime verification, identifying exact compatibility gaps.

**Key Deliverables:**
* A standalone Java/Quarkus-based test project conforming to Apicurio Registry’s main branch coding standards.
* A static analysis module comparing OpenAPI specs and validating official Confluent examples.
* A runtime verification suite utilizing `REST Assured` against containerized instances managed via `podman-compose`.
* An HTML reference report generator detailing pass/fail status and API feature gaps.
* A GitHub Actions workflow to automate execution in CI.

---

### 2. Scope & Boundaries

| Aspect | Requirement / Constraint |
| :--- | :--- |
| **API Target Version** | Confluent Schema Registry API **v8** |
| **Artifact Format** | **Avro** only (Protobuf and JSON Schema are out of scope for v1.0). |
| **Coverage** | All Confluent v8 API endpoints must be tested. Missing features in Apicurio must not crash the suite; they must be explicitly **flagged as intentional gaps/incompatibilities** in the final report. |
| **State Management** | Tests should manage their own state (e.g., seeding schemas, registering subjects, tearing down). Agents should analyze the `apicurio/apicurio-registry` GitHub repository for existing TCK (Technology Compatibility Kit) test data setup patterns. |

---

### 3. Technology Stack & Frameworks

* **Language / Framework:** Java, Quarkus (Test framework using `@QuarkusTest`).
* **Dependency Management:** Maven. Agents must dynamically extract `quarkus.version` and other core dependency versions from the Apicurio Registry `main` branch `pom.xml` to ensure absolute alignment.
* **API Testing Tool:** `REST Assured` (Standard for Quarkus API testing).
* **Container Orchestration:** `podman` and `podman-compose`.
* **Static Analysis:** Java-based OpenAPI diff tool (e.g., `openapi-diff`) integrated into the test lifecycle.
* **Reporting:** Tools like Allure or a custom Quarkus templating engine (e.g., Qute) to generate a standalone HTML reference document.

---

### 4. Architecture & Implementation Requirements

#### 4.1 Container Environment (`podman-compose.yml`)
The project must include a `podman-compose.yml` that defines:
1.  **Confluent Schema Registry v8:** Sourced from the official Confluent Docker hub registry.
2.  **Apicurio Registry:** Using the latest nightly or main branch build image.
3.  **Kafka/Zookeeper/KRaft:** Required backing services for Confluent/Apicurio to function.

#### 4.2 Static Compliance Module
Before runtime tests execute, the test suite must perform static checks:
* **OpenAPI Diff:** Fetch or store the official Confluent v8 OpenAPI spec and compare it against the Apicurio Registry OpenAPI spec (the specific Confluent-compatible REST API endpoints exposed by Apicurio).
* **Code Example Validation:** The suite must parse official Confluent Avro payload examples (from Confluent documentation/codebases) and validate them against Apicurio's API statically where applicable.

#### 4.3 Runtime Verification Suite (`REST Assured`)
Create a comprehensive test suite mapped identically to the Confluent API paths:
* `/subjects`
* `/subjects/{subject}/versions`
* `/schemas`
* `/compatibility`
* `/config`
* *And all other v8 paths.*

**Test Assertions Must Include:**
* HTTP Status Code parity.
* Response JSON body schema parity.
* Error code parity (Does Apicurio return the same Confluent specific error codes, e.g., `40401` for Subject not found?).

#### 4.4 Exception Handling & Gap Flagging
If Apicurio lacks a feature (e.g., specific schema referencing limits, or advanced mode configurations):
* Catch the failure.
* Mark the test result as `SKIPPED_GAP` or `KNOWN_INCOMPATIBILITY` rather than a standard `FAIL`.
* Log the exact deviation for the HTML report.

---

### 5. Reporting Details

The build must output an HTML report (e.g., `target/compatibility-report.html`) intended to serve as reference documentation.
**The report must contain:**
* **Summary Matrix:** Total Endpoints, Compatible, Incompatible, Known Gaps.
* **Static Diff Results:** Missing parameters, differing return types.
* **Runtime Results (Avro Specific):** A detailed table of tested endpoints, the sent payload, Confluent's response, Apicurio's response, and the differential result.

---

### 6. CI/CD Integration (GitHub Actions)

Create a `.github/workflows/compatibility-tests.yml` that dictates:
1.  Checkout repository.
2.  Install Java and Maven.
3.  Install Podman and `podman-compose`.
4.  Run `podman-compose up -d` to bootstrap the environment.
5.  Wait for health checks (ensure both registries are ready).
6.  Execute `mvn clean test`.
7.  Generate the HTML report.
8.  Upload the HTML report as a GitHub Actions Artifact for developer review.
9.  Run `podman-compose down` to tear down.