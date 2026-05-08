package dev.julianpaul.springdrift.analyzer;

import dev.julianpaul.springdrift.config.StageConfig;

import java.util.*;

/**
 * Compares configs across all stages and detects:
 * - Missing keys (key present in some stages but not others)
 * - Value drift (same key, different values across stages)
 * - Dangerous defaults (insecure values that may have leaked from dev to prod)
 */
public class DriftAnalyzer {

    private static final Map<String, Set<Object>> DANGEROUS_DEFAULTS = Map.of(
            "spring.datasource.password", Set.of("", "password", "root", "admin", "secret", "changeme"),
            "spring.security.user.password", Set.of("", "password", "admin", "secret", "changeme"),
            "management.endpoints.web.exposure.include", Set.of("*"),
            "spring.h2.console.enabled", Set.of("true", true),
            "debug", Set.of("true", true),
            "spring.jpa.show-sql", Set.of("true", true)
    );

    public record DriftEntry(
            DriftType type,
            String key,
            Map<String, Object> valuesByStage,
            String detail
    ) {}

    public enum DriftType {
        DANGEROUS_DEFAULT,
        MISSING_KEY,
        VALUE_DRIFT
    }

    public List<DriftEntry> analyze(Map<String, StageConfig> stages) {
        List<DriftEntry> results = new ArrayList<>();

        Set<String> allKeys = collectAllKeys(stages);

        for (String key : allKeys) {
            checkMissingKey(key, stages, results);
            checkValueDrift(key, stages, results);
            checkDangerousDefault(key, stages, results);
        }

        results.sort(Comparator.comparing(DriftEntry::type).thenComparing(DriftEntry::key));
        return results;
    }

    private Set<String> collectAllKeys(Map<String, StageConfig> stages) {
        Set<String> keys = new TreeSet<>();
        stages.values().forEach(s -> keys.addAll(s.properties().keySet()));
        return keys;
    }

    private void checkMissingKey(String key, Map<String, StageConfig> stages, List<DriftEntry> results) {
        List<String> presentIn = new ArrayList<>();
        List<String> missingIn = new ArrayList<>();

        for (Map.Entry<String, StageConfig> entry : stages.entrySet()) {
            if (entry.getValue().hasKey(key)) {
                presentIn.add(entry.getKey());
            } else {
                missingIn.add(entry.getKey());
            }
        }

        if (!missingIn.isEmpty() && !presentIn.isEmpty()) {
            Map<String, Object> valuesByStage = new LinkedHashMap<>();
            for (String stage : presentIn) {
                valuesByStage.put(stage, stages.get(stage).getValue(key));
            }
            for (String stage : missingIn) {
                valuesByStage.put(stage, "<missing>");
            }
            results.add(new DriftEntry(
                    DriftType.MISSING_KEY,
                    key,
                    valuesByStage,
                    "Missing in: " + String.join(", ", missingIn)
            ));
        }
    }

    private void checkValueDrift(String key, Map<String, StageConfig> stages, List<DriftEntry> results) {
        Map<String, Object> valuesByStage = new LinkedHashMap<>();
        for (Map.Entry<String, StageConfig> entry : stages.entrySet()) {
            if (entry.getValue().hasKey(key)) {
                valuesByStage.put(entry.getKey(), entry.getValue().getValue(key));
            }
        }

        if (valuesByStage.size() < 2) return;

        Set<String> distinctValues = new HashSet<>();
        valuesByStage.values().forEach(v -> distinctValues.add(String.valueOf(v)));

        if (distinctValues.size() > 1) {
            results.add(new DriftEntry(
                    DriftType.VALUE_DRIFT,
                    key,
                    valuesByStage,
                    "Different values across " + valuesByStage.size() + " stages"
            ));
        }
    }

    private void checkDangerousDefault(String key, Map<String, StageConfig> stages, List<DriftEntry> results) {
        Set<Object> dangerousValues = DANGEROUS_DEFAULTS.get(key);
        if (dangerousValues == null) return;

        Map<String, Object> offenders = new LinkedHashMap<>();
        for (Map.Entry<String, StageConfig> entry : stages.entrySet()) {
            Object value = entry.getValue().getValue(key);
            if (value != null && dangerousValues.contains(value)) {
                offenders.put(entry.getKey(), value);
            }
        }

        if (!offenders.isEmpty()) {
            results.add(new DriftEntry(
                    DriftType.DANGEROUS_DEFAULT,
                    key,
                    offenders,
                    "Dangerous value in: " + String.join(", ", offenders.keySet())
            ));
        }
    }
}
