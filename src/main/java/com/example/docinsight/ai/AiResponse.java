package com.example.docinsight.ai;

/**
 * 라우터/클라이언트의 통합 응답.
 * status로 실제 호출 성공, dry-run(키 없음), 예산 차단, 실패를 구분한다.
 */
public record AiResponse(
        AiModel model,
        Status status,
        String content,
        long inputTokens,
        long outputTokens,
        String message
) {
    public enum Status {
        SUCCESS,
        DRY_RUN,
        BUDGET_BLOCKED,
        FAILED
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public static AiResponse success(AiModel model, String content, long inputTokens, long outputTokens) {
        return new AiResponse(model, Status.SUCCESS, content, inputTokens, outputTokens, null);
    }

    public static AiResponse dryRun(AiModel model, String message) {
        return new AiResponse(model, Status.DRY_RUN, null, 0, 0, message);
    }

    public static AiResponse budgetBlocked(AiModel model, String message) {
        return new AiResponse(model, Status.BUDGET_BLOCKED, null, 0, 0, message);
    }

    public static AiResponse failed(AiModel model, String message) {
        return new AiResponse(model, Status.FAILED, null, 0, 0, message);
    }
}
