package com.example.docinsight.summary;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 스캔된 빌드 파일에서 빌드 도구/Java 버전/Spring Boot 버전을 추출한다.
 * Gradle(Kotlin/Groovy)과 Maven을 지원하며, 값이 없으면 "확인 필요"로 남긴다.
 */
@Component
public class BuildInfoExtractor {

    // Gradle: JavaLanguageVersion.of(21), languageVersion = JavaVersion.VERSION_21, sourceCompatibility = "21"
    private static final Pattern GRADLE_LANGUAGE_VERSION = Pattern.compile("JavaLanguageVersion\\.of\\((\\d+)\\)");
    private static final Pattern JAVA_VERSION_ENUM = Pattern.compile("JavaVersion\\.VERSION_(\\d+)");
    private static final Pattern SOURCE_COMPATIBILITY = Pattern.compile("sourceCompatibility\\s*=?\\s*[\"']?(\\d+)[\"']?");
    private static final Pattern SPRING_BOOT_PLUGIN = Pattern.compile("org\\.springframework\\.boot[\"']?\\s*\\)?\\s*version\\s*[\"']([^\"']+)[\"']");

    // Maven
    private static final Pattern MAVEN_JAVA_VERSION = Pattern.compile("<(?:java\\.version|maven\\.compiler\\.source|maven\\.compiler\\.release)>\\s*(\\d+)\\s*</");
    private static final Pattern MAVEN_SPRING_BOOT_PARENT = Pattern.compile(
            "spring-boot-starter-parent</artifactId>\\s*<version>\\s*([^<]+?)\\s*</version>", Pattern.DOTALL);

    public BuildInfo extract(List<Path> files) {
        Path gradleKts = findByName(files, "build.gradle.kts");
        Path gradleGroovy = findByName(files, "build.gradle");
        Path pom = findByName(files, "pom.xml");

        if (gradleKts != null || gradleGroovy != null) {
            String content = readAll(gradleKts, gradleGroovy);
            return new BuildInfo("Gradle", gradleJavaVersion(content), springBootVersion(content, SPRING_BOOT_PLUGIN));
        }
        if (pom != null) {
            String content = read(pom);
            return new BuildInfo("Maven", firstGroup(MAVEN_JAVA_VERSION, content), springBootVersion(content, MAVEN_SPRING_BOOT_PARENT));
        }
        return BuildInfo.unknown();
    }

    private String gradleJavaVersion(String content) {
        String v = firstGroup(GRADLE_LANGUAGE_VERSION, content);
        if (!v.equals(BuildInfo.UNKNOWN)) {
            return v;
        }
        v = firstGroup(JAVA_VERSION_ENUM, content);
        if (!v.equals(BuildInfo.UNKNOWN)) {
            return v;
        }
        return firstGroup(SOURCE_COMPATIBILITY, content);
    }

    private String springBootVersion(String content, Pattern pattern) {
        return firstGroup(pattern, content);
    }

    private String firstGroup(Pattern pattern, String content) {
        Matcher m = pattern.matcher(content);
        return m.find() ? m.group(1).trim() : BuildInfo.UNKNOWN;
    }

    private Path findByName(List<Path> files, String name) {
        return files.stream()
                .filter(f -> f.getFileName().toString().equals(name))
                .findFirst()
                .orElse(null);
    }

    private String readAll(Path... paths) {
        StringBuilder sb = new StringBuilder();
        for (Path p : paths) {
            if (p != null) {
                sb.append(read(p)).append('\n');
            }
        }
        return sb.toString();
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            return "";
        }
    }
}
