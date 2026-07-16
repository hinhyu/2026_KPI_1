package com.example.docinsight.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigKeyExtractorTest {

    private final ConfigKeyExtractor extractor = new ConfigKeyExtractor();

    @TempDir
    Path tempDir;

    @Test
    void extractsFlattenedYamlKeys() throws Exception {
        Path yaml = tempDir.resolve("application.yml");
        Files.writeString(yaml, """
                server:
                  port: 8080
                spring:
                  application:
                    name: sample
                """);

        List<String> keys = extractor.extract(yaml);

        assertTrue(keys.contains("server.port"));
        assertTrue(keys.contains("spring.application.name"));
    }
}
