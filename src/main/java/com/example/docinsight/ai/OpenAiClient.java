package com.example.docinsight.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

/**
 * OpenAI 호환 Chat Completions 클라이언트 (JDK HttpClient, 추가 의존성 없음).
 * base-url만 바꾸면 Cursor 등 OpenAI 호환 게이트웨이에도 사용 가능.
 */
@Component
public class OpenAiClient implements AiClient {

    private static final Set<AiModel> SUPPORTED =
            EnumSet.of(AiModel.GPT_4O_MINI, AiModel.GPT_5_5, AiModel.GPT_5_5_CODEX);

    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public OpenAiClient(AiProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public boolean supports(AiModel model) {
        return SUPPORTED.contains(model);
    }

    @Override
    public boolean isReady() {
        return properties.getOpenai().hasApiKey();
    }

    @Override
    public AiResponse generate(AiModel model, String prompt) {
        try {
            AiProperties.OpenAi openai = properties.getOpenai();
            String body = buildRequestBody(prompt);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(openai.getBaseUrl() + "/chat/completions"))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openai.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                return AiResponse.failed(model,
                        "HTTP " + response.statusCode() + ": " + truncate(response.body()));
            }
            return parseResponse(model, response.body());
        } catch (Exception e) {
            return AiResponse.failed(model, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private String buildRequestBody(String prompt) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getOpenai().getChatModel());
        ArrayNode messages = root.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", prompt);
        return objectMapper.writeValueAsString(root);
    }

    private AiResponse parseResponse(AiModel model, String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        long inputTokens = root.path("usage").path("prompt_tokens").asLong(0);
        long outputTokens = root.path("usage").path("completion_tokens").asLong(0);
        if (content.isBlank()) {
            return AiResponse.failed(model, "빈 응답: " + truncate(responseBody));
        }
        return AiResponse.success(model, content, inputTokens, outputTokens);
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
