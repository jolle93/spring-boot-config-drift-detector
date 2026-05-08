package dev.julianpaul.springdrift.report;

import dev.julianpaul.springdrift.analyzer.DriftAnalyzer;
import dev.julianpaul.springdrift.config.StageConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Path DEFAULT_OUTPUT = Path.of("drift-report.md");

    public Path generate(List<DriftAnalyzer.DriftEntry> drifts, Map<String, StageConfig> stages, Path outputFile) throws IOException {
        Path target = outputFile != null ? outputFile : DEFAULT_OUTPUT;
        String content = buildReport(drifts, stages);
        Files.writeString(target, content, StandardCharsets.UTF_8);
        return target;
    }

    private String buildReport(List<DriftAnalyzer.DriftEntry> drifts, Map<String, StageConfig> stages) {
        StringBuilder sb = new StringBuilder();

        sb.append("# Spring Boot Config Drift Report\n\n");
        sb.append("Generated: ").append(LocalDateTime.now().format(FORMATTER)).append("\n\n");

        sb.append("## Summary\n\n");
        sb.append("| Metric | Count |\n");
        sb.append("|--------|-------|\n");
        sb.append("| Stages scanned | ").append(stages.size()).append(" |\n");
        sb.append("| Total drifts | ").append(drifts.size()).append(" |\n");
        sb.append("| Missing keys | ").append(countType(drifts, DriftAnalyzer.DriftType.MISSING_KEY)).append(" |\n");
        sb.append("| Value drifts | ").append(countType(drifts, DriftAnalyzer.DriftType.VALUE_DRIFT)).append(" |\n");
        sb.append("| Dangerous defaults | ").append(countType(drifts, DriftAnalyzer.DriftType.DANGEROUS_DEFAULT)).append(" |\n\n");

        if (drifts.isEmpty()) {
            sb.append("**No drift detected.** All stages are consistent.\n");
            return sb.toString();
        }

        appendSection(sb, drifts, DriftAnalyzer.DriftType.DANGEROUS_DEFAULT, "Dangerous Defaults", "> These values are insecure and must be changed before production.");
        appendSection(sb, drifts, DriftAnalyzer.DriftType.MISSING_KEY, "Missing Keys", "> Keys present in some stages but missing in others.");
        appendSection(sb, drifts, DriftAnalyzer.DriftType.VALUE_DRIFT, "Value Drift", "> Same key, different values across stages.");

        return sb.toString();
    }

    private void appendSection(StringBuilder sb, List<DriftAnalyzer.DriftEntry> drifts, DriftAnalyzer.DriftType type, String title, String note) {
        List<DriftAnalyzer.DriftEntry> filtered = drifts.stream()
                .filter(d -> d.type() == type)
                .toList();

        if (filtered.isEmpty()) return;

        sb.append("## ").append(title).append("\n\n");
        sb.append(note).append("\n\n");

        for (DriftAnalyzer.DriftEntry entry : filtered) {
            sb.append("### `").append(entry.key()).append("`\n\n");
            sb.append("**").append(entry.detail()).append("**\n\n");
            sb.append("| Stage | Value |\n");
            sb.append("|-------|-------|\n");
            for (Map.Entry<String, Object> e : entry.valuesByStage().entrySet()) {
                sb.append("| ").append(e.getKey()).append(" | `").append(e.getValue()).append("` |\n");
            }
            sb.append("\n");
        }
    }

    private long countType(List<DriftAnalyzer.DriftEntry> drifts, DriftAnalyzer.DriftType type) {
        return drifts.stream().filter(d -> d.type() == type).count();
    }
}
