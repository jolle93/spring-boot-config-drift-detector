package dev.julianpaul.springdrift.cli;

import dev.julianpaul.springdrift.analyzer.DriftAnalyzer;
import dev.julianpaul.springdrift.config.ConfigLoader;
import dev.julianpaul.springdrift.config.StageConfig;
import dev.julianpaul.springdrift.license.GumroadLicenseChecker;
import dev.julianpaul.springdrift.license.LicenseStore;
import dev.julianpaul.springdrift.report.ReportGenerator;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@Command(
        name = "scan",
        description = "Scan a Spring Boot resources directory for config drift",
        mixinStandardHelpOptions = true
)
public class ScanCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the resources directory", defaultValue = "src/main/resources")
    private Path resourcesDir;

    @Option(names = {"-o", "--output"}, description = "Output file for the drift report (default: drift-report.md)")
    private Path outputFile;

    @Option(names = {"--fail-on-drift"}, description = "Exit with code 1 if drift is detected")
    private boolean failOnDrift;

    @Override
    public Integer call() {
        int licenseCheck = checkLicense();
        if (licenseCheck != 0) return licenseCheck;

        try {
            ConfigLoader loader = new ConfigLoader();
            Map<String, StageConfig> stages = loader.load(resourcesDir);

            if (stages.isEmpty()) {
                System.err.println("No config files found in: " + resourcesDir);
                return 1;
            }

            DriftAnalyzer analyzer = new DriftAnalyzer();
            List<DriftAnalyzer.DriftEntry> drifts = analyzer.analyze(stages);

            ReportGenerator generator = new ReportGenerator();
            Path report = generator.generate(drifts, stages, outputFile);

            System.out.println("Report written to: " + report);
            System.out.printf("Found %d drift(s) across %d stage(s)%n", drifts.size(), stages.size());

            if (failOnDrift && !drifts.isEmpty()) {
                return 1;
            }
            return 0;

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 2;
        }
    }

    private int checkLicense() {
        if ("true".equalsIgnoreCase(System.getenv("SPRING_DRIFT_SKIP_LICENSE"))) {
            return 0;
        }

        LicenseStore store = new LicenseStore();

        if (!store.hasKey()) {
            System.err.println("No license key found.");
            System.err.println("Activate your license with: spring-drift license activate <KEY>");
            System.err.println("Purchase at: https://jollepaul.gumroad.com/l/spring-drift");
            return 1;
        }

        if (store.isVerifiedRecently()) {
            return 0;
        }

        // Cache expired or missing — re-verify with Gumroad
        try {
            String key = store.load();
            GumroadLicenseChecker.VerificationResult result = new GumroadLicenseChecker().verify(key);
            if (result.success()) {
                store.saveVerified();
                return 0;
            }
            System.err.println("License invalid: " + result.message());
            System.err.println("Re-activate with: spring-drift license activate <KEY>");
            return 1;
        } catch (IOException | InterruptedException e) {
            // Fail open when offline: allow the scan if a key is present but unreachable
            if (store.hasCachedVerification()) {
                System.err.println("Warning: could not reach license server (" + e.getMessage() + "). Using cached verification.");
                return 0;
            }
            System.err.println("Could not verify license: " + e.getMessage());
            System.err.println("Please check your internet connection.");
            return 2;
        }
    }
}
