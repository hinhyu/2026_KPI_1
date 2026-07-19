package com.example.docinsight.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Google Generative Language API(Gemini) 클라이언트 (JDK HttpClient, 추가 의존성 없음).
 * OpenAI와 요청/응답 JSON 형식이 다르므로 별도 매핑한다.
 * - 요청:  { "contents": [ { "parts": [ { "text": prompt } ] } ] }
 * - 응답:  candidates[0].content.parts[0].text, usageMetadata.{promptTokenCount, candidatesTokenCount}
 */
@Component
public class GeminiClient implements AiClient {

    private final AiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public GeminiClient(AiProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public boolean supports(AiModel model) {
        return model == AiModel.GEMINI_FLASH_LITE;
    }

    @Override
    public boolean isReady() {
        return properties.getGemini().hasApiKey();
    }

    @Override
    public AiResponse generate(AiModel model, String prompt) {
        try {
            AiProperties.Gemini gemini = properties.getGemini();
            String url = gemini.getBaseUrl() + "/models/" + gemini.getModel() + ":generateContent";
            String body = buildRequestBody(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", gemini.getApiKey())
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
        root.putArray("contents")
                .addObject()
                .putArray("parts")
                .addObject()
                .put("text", prompt);
        return objectMapper.writeValueAsString(root);
    }

    private AiResponse parseResponse(AiModel model, String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String content = root.path("candidates").path(0)
                .path("content").path("parts").path(0).path("text").asText("");
        long inputTokens = root.path("usageMetadata").path("promptTokenCount").asLong(0);
        long outputTokens = root.path("usageMetadata").path("candidatesTokenCount").asLong(0);
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
