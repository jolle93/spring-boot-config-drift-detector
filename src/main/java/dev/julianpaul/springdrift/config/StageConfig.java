package dev.julianpaul.springdrift.config;

import java.util.Collections;
import java.util.Map;

public record StageConfig(String stage, Map<String, Object> properties) {

    public StageConfig {
        properties = Collections.unmodifiableMap(properties);
    }

    public boolean hasKey(String flatKey) {
        return properties.containsKey(flatKey);
    }

    public Object getValue(String flatKey) {
        return properties.get(flatKey);
    }
}
