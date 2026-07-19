package com.example.docinsight.ai;

/**
 * 제공자 추상화. Gemini/OpenAI 등 각 제공자가 구현하며,
 * 라우터는 모델을 지원하는 클라이언트를 골라 위임한다.
 */
public interface AiClient {

    boolean supports(AiModel model);

    /** API 키 등 호출 준비가 됐는지. false면 라우터가 dry-run으로 처리한다. */
    boolean isReady();

    /** 실제 API 호출. 성공/실패는 AiResponse.status로 표현한다. */
    AiResponse generate(AiModel model, String prompt);
}
