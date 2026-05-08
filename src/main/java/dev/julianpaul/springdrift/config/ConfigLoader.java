package dev.julianpaul.springdrift.config;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Loads application*.yml/yaml and application*.properties from a resources directory.
 * Returns a map of stage name → flat key/value config.
 * Stage name "default" = application.yml / application.properties.
 */
public class ConfigLoader {

    private static final PathMatcher YAML_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:application*.{yml,yaml}");
    private static final PathMatcher PROPS_MATCHER =
            FileSystems.getDefault().getPathMatcher("glob:application*.properties");

    public Map<String, StageConfig> load(Path resourcesDir) throws IOException {
        if (!Files.isDirectory(resourcesDir)) {
            throw new IOException("Not a directory: " + resourcesDir.toAbsolutePath());
        }

        Map<String, StageConfig> result = new LinkedHashMap<>();

        List<Path> files = collectFiles(resourcesDir);
        for (Path file : files) {
            String stage = extractStage(file.getFileName().toString());
            Map<String, Object> props;

            if (YAML_MATCHER.matches(file.getFileName())) {
                props = loadYaml(file);
            } else {
                props = loadProperties(file);
            }

            Map<String, Object> flat = flatten(props);
            result.merge(stage, new StageConfig(stage, flat),
                    (a, b) -> new StageConfig(stage, merged(a.properties(), b.properties())));
        }

        return result;
    }

    private List<Path> collectFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> all = Files.find(dir, 1, (p, a) -> {
            Path name = p.getFileName();
            return YAML_MATCHER.matches(name) || PROPS_MATCHER.matches(name);
        })) {
            all.forEach(files::add);
        }
        files.sort(Comparator.comparing(p -> p.getFileName().toString()));
        return files;
    }

    // application.yml → "default", application-prod.yml → "prod"
    String extractStage(String filename) {
        String name = filename.replaceAll("\\.(yml|yaml|properties)$", "");
        if (name.equals("application")) {
            return "default";
        }
        return name.replaceFirst("^application-", "");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYaml(Path file) throws IOException {
        Yaml yaml = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> merged = new LinkedHashMap<>();
        try (InputStream in = Files.newInputStream(file)) {
            // loadAll handles multi-document YAML files (--- separator)
            for (Object doc : yaml.loadAll(in)) {
                if (doc instanceof Map<?, ?> map) {
                    merged.putAll((Map<String, Object>) map);
                }
            }
        }
        return merged;
    }

    private Map<String, Object> loadProperties(Path file) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            props.load(in);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames()) {
            result.put(key, props.getProperty(key));
        }
        return result;
    }

    // Recursively flattens nested maps: {server: {port: 8080}} → {"server.port": 8080}
    Map<String, Object> flatten(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        flattenRecursive("", map, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private void flattenRecursive(String prefix, Map<String, Object> map, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            if (entry.getValue() instanceof Map<?, ?> nested) {
                flattenRecursive(key, (Map<String, Object>) nested, result);
            } else {
                result.put(key, entry.getValue());
            }
        }
    }

    private Map<String, Object> merged(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> merged = new LinkedHashMap<>(a);
        merged.putAll(b);
        return merged;
    }
}
