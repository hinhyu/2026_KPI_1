package com.example.docinsight.ai;

/**
 * ai_request_log 한 건 (PPT 슬라이드12/13).
 * 1차 MVP는 파일(JSON)로 저장하고, 2차에서 DB로 확장한다.
 */
public record AiRequestLogEntry(
        String timestamp,
        String task,
        String model,
        String status,
        long inputTokens,
        long outputTokens,
        double costUsd,
        double costKrw,
        String message
) {}
