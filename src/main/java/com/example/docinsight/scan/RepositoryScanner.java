package com.example.docinsight.scan;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

@Component
public class RepositoryScanner {

    private static final List<String> EXCLUDE_DIRS = List.of(
            ".git", ".gradle", "build", "target", "node_modules", "out", "logs", ".idea"
    );

    public List<Path> scan(Path root) {
        try (Stream<Path> paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(this::isTargetFile)
                    .filter(this::isNotExcluded)
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Repository scan failed: " + root, e);
        }
    }

    private boolean isTargetFile(Path path) {
        String fileName = path.getFileName().toString();
        String pathText = path.toString().replace("\\", "/");

        return fileName.endsWith(".java")
                || fileName.equals("application.yml")
                || fileName.equals("application.yaml")
                || (fileName.startsWith("application-") && (fileName.endsWith(".yml") || fileName.endsWith(".yaml")))
                || fileName.equals("application.properties")
                || fileName.equals("build.gradle")
                || fileName.equals("build.gradle.kts")
                || fileName.equals("settings.gradle")
                || fileName.equals("settings.gradle.kts")
                || fileName.equals("pom.xml")
                || fileName.equalsIgnoreCase("README.md")
                || (pathText.contains("/docs/") && fileName.endsWith(".md"));
    }

    private boolean isNotExcluded(Path path) {
        for (Path part : path) {
            if (EXCLUDE_DIRS.contains(part.toString())) {
                return false;
            }
        }
        return true;
    }
}
