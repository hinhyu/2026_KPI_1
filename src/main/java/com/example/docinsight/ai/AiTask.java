package com.example.docinsight.ai;

/**
 * 작업 유형 → 기본 모델 라우팅 (PPT 슬라이드5/12).
 * 초안/요약은 저비용 모델, 고위험 리뷰는 Codex급, 최종 보강은 상위 모델로 분리한다.
 * 현재는 OpenAI만 연동되어 있어 초안 작업을 저비용 gpt-4o-mini로 매핑하며,
 * Gemini 연동 후 ONBOARDING_DRAFT의 기본 모델을 GEMINI_FLASH_LITE로 되돌린다.
 */
public enum AiTask {
    ONBOARDING_DRAFT(AiModel.GPT_4O_MINI),
    API_GUIDE_DRAFT(AiModel.GPT_4O_MINI),
    CONFIG_GUIDE_DRAFT(AiModel.GPT_4O_MINI),
    HIGH_RISK_REVIEW(AiModel.GPT_5_5_CODEX),
    FINAL_POLISH(AiModel.GPT_5_5),
    EMBEDDING(AiModel.EMBEDDING_SMALL);

    private final AiModel defaultModel;

    AiTask(AiModel defaultModel) {
        this.defaultModel = defaultModel;
    }

    public AiModel defaultModel() {
        return defaultModel;
    }
}
