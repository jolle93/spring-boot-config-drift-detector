package dev.julianpaul.springdrift.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigLoaderTest {

    private final ConfigLoader loader = new ConfigLoader();

    @Test
    void extractStage_defaultForBaseFile() {
        assertThat(loader.extractStage("application.yml")).isEqualTo("default");
        assertThat(loader.extractStage("application.properties")).isEqualTo("default");
    }

    @Test
    void extractStage_namedStage() {
        assertThat(loader.extractStage("application-prod.yml")).isEqualTo("prod");
        assertThat(loader.extractStage("application-dev.properties")).isEqualTo("dev");
    }

    @Test
    void flatten_nestedMap() {
        Map<String, Object> input = Map.of(
                "server", Map.of("port", 8080, "host", "localhost"),
                "spring", Map.of("datasource", Map.of("url", "jdbc:h2:mem"))
        );

        Map<String, Object> flat = loader.flatten(input);

        assertThat(flat).containsEntry("server.port", 8080)
                .containsEntry("server.host", "localhost")
                .containsEntry("spring.datasource.url", "jdbc:h2:mem");
    }

    @Test
    void load_parsesYamlFile(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("application.yml"), """
                server:
                  port: 8080
                spring:
                  application:
                    name: my-app
                """);

        Map<String, StageConfig> result = loader.load(dir);

        assertThat(result).containsKey("default");
        assertThat(result.get("default").getValue("server.port")).isEqualTo(8080);
        assertThat(result.get("default").getValue("spring.application.name")).isEqualTo("my-app");
    }

    @Test
    void load_parsesPropertiesFile(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("application-prod.properties"), """
                server.port=9090
                spring.datasource.url=jdbc:postgresql://prod-db/app
                """);

        Map<String, StageConfig> result = loader.load(dir);

        assertThat(result).containsKey("prod");
        assertThat(result.get("prod").getValue("server.port")).isEqualTo("9090");
    }

    @Test
    void load_multipleStages(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("application.yml"), "debug: false\n");
        Files.writeString(dir.resolve("application-dev.yml"), "debug: true\n");
        Files.writeString(dir.resolve("application-prod.yml"), "debug: false\n");

        Map<String, StageConfig> result = loader.load(dir);

        assertThat(result).containsKeys("default", "dev", "prod");
    }

    @Test
    void load_throwsForNonDirectory() {
        assertThatThrownBy(() -> loader.load(Path.of("/nonexistent/path")))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Not a directory");
    }
}
