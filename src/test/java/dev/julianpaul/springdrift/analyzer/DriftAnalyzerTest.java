package dev.julianpaul.springdrift.analyzer;

import dev.julianpaul.springdrift.config.StageConfig;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DriftAnalyzerTest {

    private final DriftAnalyzer analyzer = new DriftAnalyzer();

    @Test
    void detectsMissingKey() {
        Map<String, StageConfig> stages = Map.of(
                "default", new StageConfig("default", Map.of("server.port", 8080, "debug", false)),
                "prod", new StageConfig("prod", Map.of("server.port", 8080))
        );

        List<DriftAnalyzer.DriftEntry> drifts = analyzer.analyze(stages);

        assertThat(drifts).anyMatch(d ->
                d.type() == DriftAnalyzer.DriftType.MISSING_KEY && d.key().equals("debug")
        );
    }

    @Test
    void detectsValueDrift() {
        Map<String, StageConfig> stages = Map.of(
                "default", new StageConfig("default", Map.of("server.port", 8080)),
                "prod", new StageConfig("prod", Map.of("server.port", 9090))
        );

        List<DriftAnalyzer.DriftEntry> drifts = analyzer.analyze(stages);

        assertThat(drifts).anyMatch(d ->
                d.type() == DriftAnalyzer.DriftType.VALUE_DRIFT && d.key().equals("server.port")
        );
    }

    @Test
    void detectsDangerousDefault_password() {
        Map<String, StageConfig> stages = Map.of(
                "prod", new StageConfig("prod", Map.of(
                        "spring.datasource.password", "password"
                ))
        );

        List<DriftAnalyzer.DriftEntry> drifts = analyzer.analyze(stages);

        assertThat(drifts).anyMatch(d ->
                d.type() == DriftAnalyzer.DriftType.DANGEROUS_DEFAULT
                        && d.key().equals("spring.datasource.password")
        );
    }

    @Test
    void detectsDangerousDefault_wildcardActuator() {
        Map<String, StageConfig> stages = Map.of(
                "prod", new StageConfig("prod", Map.of(
                        "management.endpoints.web.exposure.include", "*"
                ))
        );

        List<DriftAnalyzer.DriftEntry> drifts = analyzer.analyze(stages);

        assertThat(drifts).anyMatch(d ->
                d.type() == DriftAnalyzer.DriftType.DANGEROUS_DEFAULT
                        && d.key().equals("management.endpoints.web.exposure.include")
        );
    }

    @Test
    void noDriftWhenAllConsistent() {
        Map<String, StageConfig> stages = Map.of(
                "default", new StageConfig("default", Map.of("server.port", 8080)),
                "prod", new StageConfig("prod", Map.of("server.port", 8080))
        );

        List<DriftAnalyzer.DriftEntry> drifts = analyzer.analyze(stages);

        assertThat(drifts).isEmpty();
    }

    @Test
    void resultsAreSortedByTypeAndKey() {
        Map<String, StageConfig> stages = Map.of(
                "default", new StageConfig("default", Map.of(
                        "spring.datasource.password", "password",
                        "server.port", 8080,
                        "debug", true
                )),
                "prod", new StageConfig("prod", Map.of(
                        "spring.datasource.password", "secret",
                        "server.port", 9090
                ))
        );

        List<DriftAnalyzer.DriftEntry> drifts = analyzer.analyze(stages);

        assertThat(drifts).isNotEmpty();
        // DANGEROUS_DEFAULT sorts before MISSING_KEY sorts before VALUE_DRIFT
        List<DriftAnalyzer.DriftType> types = drifts.stream().map(DriftAnalyzer.DriftEntry::type).toList();
        assertThat(types).isSortedAccordingTo(Enum::compareTo);
    }
}
