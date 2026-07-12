package com.example.docinsight.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Component
public class ConfigKeyExtractor {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    public List<String> extract(Path configFile) {
        String name = configFile.getFileName().toString();
        try {
            if (name.endsWith(".properties")) {
                return extractProperties(configFile);
            }
            if (name.endsWith(".yml") || name.endsWith(".yaml")) {
                return extractYaml(configFile);
            }
            return List.of();
        } catch (Exception e) {
            return List.of("[parse-failed] " + name + " : " + e.getMessage());
        }
    }

    private List<String> extractProperties(Path file) throws IOException {
        Properties props = new Properties();
        try (var in = Files.newInputStream(file)) {
            props.load(in);
        }
        return props.keySet().stream().map(String::valueOf).sorted().toList();
    }

    @SuppressWarnings("unchecked")
    private List<String> extractYaml(Path file) throws IOException {
        Object value = yamlMapper.readValue(file.toFile(), Object.class);
        List<String> keys = new ArrayList<>();
        flatten("", value, keys);
        return keys.stream().sorted().toList();
    }

    @SuppressWarnings("unchecked")
    private void flatten(String prefix, Object value, List<String> keys) {
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = prefix.isBlank() ? String.valueOf(entry.getKey()) : prefix + "." + entry.getKey();
                flatten(key, entry.getValue(), keys);
            }
        } else if (!prefix.isBlank()) {
            keys.add(prefix);
        }
    }
}
