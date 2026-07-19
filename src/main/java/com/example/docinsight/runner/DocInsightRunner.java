package com.example.docinsight.runner;

import com.example.docinsight.ai.AiRequest;
import com.example.docinsight.ai.AiResponse;
import com.example.docinsight.ai.AiTask;
import com.example.docinsight.ai.ModelRouter;
import com.example.docinsight.api.ApiEndpointInfo;
import com.example.docinsight.api.EndpointExtractor;
import com.example.docinsight.config.ConfigKeyExtractor;
import com.example.docinsight.java.JavaClassInfo;
import com.example.docinsight.java.JavaStructureAnalyzer;
import com.example.docinsight.scan.RepositoryScanner;
import com.example.docinsight.summary.BuildInfo;
import com.example.docinsight.summary.BuildInfoExtractor;
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
    private final BuildInfoExtractor buildInfoExtractor;
    private final SummaryBuilder summaryBuilder;
    private final ModelRouter modelRouter;
    private final ObjectMapper objectMapper;

    public DocInsightRunner(
            RepositoryScanner scanner,
            JavaStructureAnalyzer javaAnalyzer,
            EndpointExtractor endpointExtractor,
            ConfigKeyExtractor configKeyExtractor,
            BuildInfoExtractor buildInfoExtractor,
            SummaryBuilder summaryBuilder,
            ModelRouter modelRouter
    ) {
        this.scanner = scanner;
        this.javaAnalyzer = javaAnalyzer;
        this.endpointExtractor = endpointExtractor;
        this.configKeyExtractor = configKeyExtractor;
        this.buildInfoExtractor = buildInfoExtractor;
        this.summaryBuilder = summaryBuilder;
        this.modelRouter = modelRouter;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!args.containsOption("repoPath")) {
            System.out.println("Usage: ./gradlew bootRun --args='--repoPath=/path/to/repository'");
            return;
        }

        Path root = Path.of(args.getOptionValues("repoPath").getFirst()).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            System.err.println("repoPath is not a directory: " + root);
            return;
        }

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
                    classes.addAll(javaAnalyzer.analyze(root, file));
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

        BuildInfo buildInfo = buildInfoExtractor.extract(files);
        ProjectSummary summary = summaryBuilder.build(root, buildInfo, classes, endpoints, configKeysByFile, markdownFiles);

        Path summaryPath = outputDir.resolve("project-summary.json");
        Path endpointPath = outputDir.resolve("api-endpoints.json");
        Path promptPath = outputDir.resolve("onboarding-prompt.md");

        String prompt = buildPrompt(summary, endpoints);
        objectMapper.writeValue(summaryPath.toFile(), summary);
        objectMapper.writeValue(endpointPath.toFile(), endpoints);
        Files.writeString(promptPath, prompt);

        System.out.println("Done.");
        System.out.println("- scanned files: " + files.size());
        System.out.println("- java types: " + classes.size());
        System.out.println("- api endpoints: " + endpoints.size());
        System.out.println("- " + summaryPath.toAbsolutePath());
        System.out.println("- " + endpointPath.toAbsolutePath());
        System.out.println("- " + promptPath.toAbsolutePath());

        generateOnboardingDoc(prompt, outputDir);
    }

    private void generateOnboardingDoc(String prompt, Path outputDir) throws Exception {
        AiResponse response = modelRouter.run(new AiRequest(AiTask.ONBOARDING_DRAFT, prompt), outputDir);

        switch (response.status()) {
            case SUCCESS -> {
                Path docPath = outputDir.resolve("onboarding.md");
                Files.writeString(docPath, response.content());
                System.out.println("- " + docPath.toAbsolutePath()
                        + " (AI 생성, in=" + response.inputTokens()
                        + " out=" + response.outputTokens() + " tokens)");
            }
            case DRY_RUN -> System.out.println("- onboarding.md 미생성 (dry-run): " + response.message());
            case BUDGET_BLOCKED -> System.out.println("- onboarding.md 미생성 (예산 차단): " + response.message());
            case FAILED -> System.err.println("- onboarding.md 생성 실패: " + response.message());
        }
    }

    private String buildPrompt(ProjectSummary summary, List<ApiEndpointInfo> endpoints) throws Exception {
        String summaryJson = objectMapper.writeValueAsString(summary);
        String endpointsJson = objectMapper.writeValueAsString(endpoints);
        return """
                너는 Java Spring Boot 프로젝트 온보딩 문서를 작성하는 개발 리더다.

                아래 프로젝트 분석 JSON을 기반으로 신규 개발자를 위한 Markdown 문서를 작성해라.

                주의사항:
                - 추측하지 말 것. 특히 빌드 도구, 언어 버전, 실행 명령을 임의로 지어내지 말 것
                - 기술 스택과 실행 방법은 반드시 buildInfo(buildTool, javaVersion, springBootVersion) 값만 근거로 작성할 것
                - buildTool이 \"Gradle\"이면 실행 명령은 ./gradlew 계열을 쓰고, \"Maven\"이면 ./mvnw 계열을 쓸 것. buildTool이 \"확인 필요\"면 실행 명령을 지어내지 말고 \"확인 필요\"로 둘 것
                - JSON에 없는 정보 또는 값이 \"확인 필요\"인 항목은 문서에도 \"확인 필요\"로 표시할 것
                - 기술 스택, 실행 방법, 주요 패키지, 주요 API, 설정 파일, 개발 시 주의사항을 포함할 것
                - API 목록은 표로 작성하고 HTTP Method, Path, Handler, Request/Response, PathVariable, RequestParam을 포함할 것
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

                프로젝트 요약 JSON:
                ```json
                %s
                ```

                API 엔드포인트 JSON:
                ```json
                %s
                ```
                """.formatted(summaryJson, endpointsJson);
    }
}
