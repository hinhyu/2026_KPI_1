package com.example.docinsight.ai;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

/**
 * AI 라우팅 진입점 (PPT ai-router).
 * task → 모델 선택 → 예산 체크 → 제공자 위임 → 요청 로그 기록.
 * enabled=false 또는 API 키가 없으면 실제 호출 없이 dry-run으로 응답한다.
 */
@Component
public class ModelRouter {

    private final List<AiClient> clients;
    private final AiProperties properties;
    private final BudgetGuard budgetGuard;
    private final AiRequestLog requestLog;

    public ModelRouter(
            List<AiClient> clients,
            AiProperties properties,
            BudgetGuard budgetGuard,
            AiRequestLog requestLog
    ) {
        this.clients = clients;
        this.properties = properties;
        this.budgetGuard = budgetGuard;
        this.requestLog = requestLog;
    }

    public AiResponse run(AiRequest request, Path outputDir) {
        AiModel model = properties.resolveModel(request.task());

        AiResponse response = dispatch(request, model, outputDir);
        log(request, response, outputDir);
        return response;
    }

    private AiResponse dispatch(AiRequest request, AiModel model, Path outputDir) {
        if (!properties.isEnabled()) {
            return AiResponse.dryRun(model, "AI 비활성(enabled=false) — 프롬프트만 생성 (dry-run).");
        }

        AiClient client = clients.stream()
                .filter(c -> c.supports(model))
                .findFirst()
                .orElse(null);
        if (client == null) {
            return AiResponse.failed(model, "지원하는 클라이언트 없음: " + model);
        }

        if (!client.isReady()) {
            return AiResponse.dryRun(model,
                    model + " API 키 없음 — 프롬프트만 생성 (dry-run).");
        }

        BudgetGuard.Decision decision = budgetGuard.check(model, outputDir);
        if (!decision.allowed()) {
            return AiResponse.budgetBlocked(model, decision.reason());
        }

        return client.generate(model, request.prompt());
    }

    private void log(AiRequest request, AiResponse response, Path outputDir) {
        double costUsd = response.model().costUsd(response.inputTokens(), response.outputTokens());
        double costKrw = costUsd * properties.getUsdToKrw();
        requestLog.append(outputDir, new AiRequestLogEntry(
                AiRequestLog.now(),
                request.task().name(),
                response.model().name(),
                response.status().name(),
                response.inputTokens(),
                response.outputTokens(),
                costUsd,
                costKrw,
                response.message()
        ));
    }
}
