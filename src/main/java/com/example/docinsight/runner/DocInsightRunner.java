package com.example.docinsight.runner;

import com.example.docinsight.api.ApiEndpointInfo;
import com.example.docinsight.api.EndpointExtractor;
import com.example.docinsight.config.ConfigKeyExtractor;
import com.example.docinsight.java.JavaClassInfo;
import com.example.docinsight.java.JavaStructureAnalyzer;
import com.example.docinsight.scan.RepositoryScanner;
import com.example.docinsight.summary.ProjectSummary;
import com.example.docinsight.summary.SummaryBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DocInsightRunner implements ApplicationRunner {

    private final RepositoryScanner scanner;
    private final JavaStructureAnalyzer javaAnalyzer;
    private final EndpointExtractor endpointExtractor;
    private final ConfigKeyExtractor configKeyExtractor;
    private final SummaryBuilder summaryBuilder;
    private final ObjectMapper objectMapper;

    public DocInsightRunner(
            RepositoryScanner scanner,
            JavaStructureAnalyzer javaAnalyzer,
            EndpointExtractor endpointExtractor,
            ConfigKeyExtractor configKeyExtractor,
            SummaryBuilder summaryBuilder
    ) {
        this.scanner = scanner;
        this.javaAnalyzer = javaAnalyzer;
        this.endpointExtractor = endpointExtractor;
        this.configKeyExtractor = configKeyExtractor;
        this.summaryBuilder = summaryBuilder;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("repoPath")) {
            System.out.println("Usage: ./gradlew bootRun --args='--repoPath=/path/to/repository'");
            return;
        }

        Path root = Path.of(args.getOptionValues("repoPath").getFirst()).toAbsolutePath().normalize();
        Path outputDir = Path.of(args.getOptionValues("outputDir") == null ? "outputs" : args.getOptionValues("outputDir").getFirst());
        Files.createDirectories(outputDir);

        List<Path> files = scanner.scan(root);
        List<JavaClassInfo> classes = new ArrayList<>();
        List<ApiEndpointInfo> endpoints = new ArrayList<>();
        Map<String, List<String>> configKeysByFile = new LinkedHashMap<>();
        List<Path> markdownFiles = new ArrayList<>();

        for (Path file : files) {
            String fileName = file.getFileName().toString();
            if (fileName.endsWith(".java")) {
                try {
                    classes.add(javaAnalyzer.analyze(root, file));
                    endpoints.addAll(endpointExtractor.extract(root, file));
                } catch (Exception e) {
                    System.out.println("[WARN] Java parse skipped: " + root.relativize(file) + " / " + e.getMessage());
                }
            } else if (fileName.endsWith(".yml") || fileName.endsWith(".yaml") || fileName.endsWith(".properties")) {
                configKeysByFile.put(root.relativize(file).toString(), configKeyExtractor.extract(file));
            } else if (fileName.endsWith(".md")) {
                markdownFiles.add(file);
            }
        }

        ProjectSummary summary = summaryBuilder.build(root, classes, endpoints, configKeysByFile, markdownFiles);

        Path summaryPath = outputDir.resolve("project-summary.json");
        Path endpointPath = outputDir.resolve("api-endpoints.json");
        Path promptPath = outputDir.resolve("onboarding-prompt.md");

        objectMapper.writeValue(summaryPath.toFile(), summary);
        objectMapper.writeValue(endpointPath.toFile(), endpoints);
        Files.writeString(promptPath, buildPrompt(summary));

        System.out.println("Done.");
        System.out.println("- " + summaryPath.toAbsolutePath());
        System.out.println("- " + endpointPath.toAbsolutePath());
        System.out.println("- " + promptPath.toAbsolutePath());
    }

    private String buildPrompt(ProjectSummary summary) throws Exception {
        String json = objectMapper.writeValueAsString(summary);
        return """
                너는 Java Spring Boot 프로젝트 온보딩 문서를 작성하는 개발 리더다.

                아래 프로젝트 분석 JSON을 기반으로 신규 개발자를 위한 Markdown 문서를 작성해라.

                주의사항:
                - 추측하지 말 것
                - JSON에 없는 정보는 \"확인 필요\"로 표시할 것
                - 기술 스택, 실행 방법, 주요 패키지, 주요 API, 설정 파일, 개발 시 주의사항을 포함할 것
                - API 목록은 표로 작성할 것
                - 문서는 실무 개발자가 바로 읽을 수 있게 간결하게 작성할 것

                문서 목차:
                1. 프로젝트 개요
                2. 기술 스택
                3. 실행 방법
                4. 패키지 구조
                5. 주요 클래스 구조
                6. 주요 API
                7. 설정 파일
                8. 신규 개발자 주의사항
                9. 확인 필요 항목

                프로젝트 분석 JSON:
                ```json
                %s
                ```
                """.formatted(json);
    }
}
