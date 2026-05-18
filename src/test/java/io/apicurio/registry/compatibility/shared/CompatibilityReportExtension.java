package io.apicurio.registry.compatibility.shared;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.apicurio.registry.compatibility.collector.TestResultCollector;
import io.apicurio.registry.compatibility.report.ContainerVersionDetector;
import io.apicurio.registry.compatibility.report.HtmlReportGenerator;
import io.apicurio.registry.compatibility.report.ReportContextEnricher;

/**
 * JUnit 5 extension that generates the HTML compatibility report after all tests complete.
 * Uses a JVM shutdown hook to defer report generation until every test class has finished,
 * since {@code afterAll()} fires per-class and we can't control class execution order.
 */
public class CompatibilityReportExtension implements BeforeAllCallback, AfterAllCallback {

    private static final String REPORT_PATH = "target/compatibility-report.html";
    private static final String KEY = "report-hook-registered";

    @Override
    public void beforeAll(ExtensionContext context) {
        ExtensionContext.Store rootStore = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        rootStore.getOrComputeIfAbsent(KEY, k -> {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    TestResultCollector collector = TestResultCollector.getInstance();
                    ReportContextEnricher enricher = new ReportContextEnricher();
                    enricher.enrich(collector.getOutcomesForEnrichment());

                    ContainerVersionDetector versionDetector = new ContainerVersionDetector();
                    ContainerVersionDetector.ContainerVersions versions = versionDetector.detect();
                    logVersionInfo(versions);

                    HtmlReportGenerator generator = new HtmlReportGenerator(collector, versions);
                    generator.generate(Path.of(REPORT_PATH));
                    System.out.println("Compatibility report written to " + REPORT_PATH
                            + " (" + collector.getTotalCount() + " results)");
                } catch (IOException e) {
                    System.err.println("Failed to generate compatibility report: " + e.getMessage());
                }
            }));
            return "done";
        });
    }

    @Override
    public void afterAll(ExtensionContext context) {
        // Report generation deferred to shutdown hook registered in beforeAll
    }

    private void logVersionInfo(ContainerVersionDetector.ContainerVersions versions) {
        if (versions == null) return;
        logImage("Apicurio Registry image", versions.apicurio());
        logImage("Confluent SR image", versions.confluent());
    }

    private void logImage(String label, ContainerVersionDetector.ImageInfo info) {
        if (info == null) return;
        StringBuilder msg = new StringBuilder(label).append(": ").append(info.fullImage());
        if (info.tag() != null) msg.append(" (tag: ").append(info.tag()).append(")");
        if (!info.shortDigest().isEmpty()) msg.append(" (sha256:").append(info.shortDigest()).append("...)");
        System.out.println(msg);
    }
}
