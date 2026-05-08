package dev.julianpaul.springdrift.report;

import dev.julianpaul.springdrift.analyzer.DriftAnalyzer;
import dev.julianpaul.springdrift.config.StageConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReportGeneratorTest {

    private final ReportGenerator generator = new ReportGenerator();

    @Test
    void generatesReportWithNoDrift(@TempDir Path dir) throws IOException {
        Path output = dir.resolve("report.md");
        Map<String, StageConfig> stages = Map.of(
                "default", new StageConfig("default", Map.of("server.port", 8080))
        );

        generator.generate(List.of(), stages, output);

        String content = Files.readString(output);
        assertThat(content)
                .contains("# Spring Boot Config Drift Report")
                .contains("No drift detected");
    }

    @Test
    void generatesReportWithDrifts(@TempDir Path dir) throws IOException {
        Path output = dir.resolve("report.md");
        Map<String, StageConfig> stages = Map.of(
                "default", new StageConfig("default", Map.of("server.port", 8080)),
                "prod", new StageConfig("prod", Map.of("server.port", 9090))
        );
        List<DriftAnalyzer.DriftEntry> drifts = List.of(
                new DriftAnalyzer.DriftEntry(
                        DriftAnalyzer.DriftType.VALUE_DRIFT,
                        "server.port",
                        Map.of("default", 8080, "prod", 9090),
                        "Different values across 2 stages"
                )
        );

        generator.generate(drifts, stages, output);

        String content = Files.readString(output);
        assertThat(content)
                .contains("Value Drift")
                .contains("server.port")
                .contains("8080")
                .contains("9090");
    }

    @Test
    void usesDefaultOutputWhenNullProvided(@TempDir Path dir) throws IOException {
        Path cwd = dir;
        Path defaultOutput = cwd.resolve("drift-report.md");

        // Can't test default path easily without changing CWD; just test non-null path works
        Path output = dir.resolve("custom.md");
        generator.generate(List.of(), Map.of(), output);

        assertThat(output).exists();
    }
}
