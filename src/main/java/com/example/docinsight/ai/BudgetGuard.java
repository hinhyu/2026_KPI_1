package com.example.docinsight.ai;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Set;

/**
 * 월 예산 통제 (PPT 슬라이드12).
 * - 월 예산 100% 도달: 모든 모델 차단
 * - 월 예산 80% 도달: 고비용 모델(GPT-5.5/Codex) 차단, 저비용 모델은 허용
 */
@Component
public class BudgetGuard {

    private static final double HIGH_COST_THRESHOLD = 0.80;
    private static final Set<AiModel> HIGH_COST_MODELS =
            EnumSet.of(AiModel.GPT_5_5, AiModel.GPT_5_5_CODEX);

    private final AiProperties properties;
    private final AiRequestLog requestLog;

    public BudgetGuard(AiProperties properties, AiRequestLog requestLog) {
        this.properties = properties;
        this.requestLog = requestLog;
    }

    public Decision check(AiModel model, Path outputDir) {
        double budget = properties.getMonthlyBudgetKrw();
        double used = requestLog.monthlyCostKrw(outputDir);

        if (used >= budget) {
            return Decision.block(
                    "월 예산 소진 (%.0f/%.0f원). 모든 모델 차단.".formatted(used, budget));
        }
        if (HIGH_COST_MODELS.contains(model) && used >= budget * HIGH_COST_THRESHOLD) {
            return Decision.block(
                    "월 예산 80%% 도달 (%.0f/%.0f원). 고비용 모델 차단, 저비용 모델만 허용."
                            .formatted(used, budget));
        }
        return Decision.allow();
    }

    public record Decision(boolean allowed, String reason) {
        static Decision allow() {
            return new Decision(true, null);
        }

        static Decision block(String reason) {
            return new Decision(false, reason);
        }
    }
}
