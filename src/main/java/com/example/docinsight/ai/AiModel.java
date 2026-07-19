package com.example.docinsight.ai;

/**
 * 라우팅/과금/로그에 사용하는 모델 식별자.
 * 단가는 1M 토큰당 USD (PPT 슬라이드11 기준, 1 USD = 1,500원으로 역산).
 * 실제 API 호출에 쓰는 모델 문자열은 provider 설정(chat-model 등)에서 별도로 지정한다.
 */
public enum AiModel {
    GEMINI_FLASH_LITE(0.10, 0.40),
    GPT_4O_MINI(0.15, 0.60),
    GPT_5_5(5.00, 30.00),
    GPT_5_5_CODEX(5.00, 30.00),
    EMBEDDING_SMALL(0.02, 0.0);

    private final double usdPerMillionInput;
    private final double usdPerMillionOutput;

    AiModel(double usdPerMillionInput, double usdPerMillionOutput) {
        this.usdPerMillionInput = usdPerMillionInput;
        this.usdPerMillionOutput = usdPerMillionOutput;
    }

    public double costUsd(long inputTokens, long outputTokens) {
        return inputTokens / 1_000_000.0 * usdPerMillionInput
                + outputTokens / 1_000_000.0 * usdPerMillionOutput;
    }
}
