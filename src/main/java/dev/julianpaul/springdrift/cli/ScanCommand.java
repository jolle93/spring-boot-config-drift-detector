package dev.julianpaul.springdrift.cli;

import dev.julianpaul.springdrift.analyzer.DriftAnalyzer;
import dev.julianpaul.springdrift.config.ConfigLoader;
import dev.julianpaul.springdrift.config.StageConfig;
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
}
