package com.example.docinsight.ai;

/**
 * 라우터에 전달되는 요청. prompt는 이미 구조화된 요약 JSON 기반 텍스트이며,
 * 코드 원문은 포함하지 않는다 (PPT 슬라이드6 핵심 원칙).
 */
public record AiRequest(AiTask task, String prompt) {}
