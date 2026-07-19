package com.example.docinsight.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "doc-insight.ai")
public class AiProperties {

    /** false면 실제 호출 없이 프롬프트만 생성한다. */
    private boolean enabled = true;

    private double usdToKrw = 1500;

    /** 월 AI API 총예산 (PPT 기준 120,000원). */
    private double monthlyBudgetKrw = 120000;

    private int timeoutSeconds = 60;

    /** 작업→모델 라우팅 오버라이드. 비어 있으면 AiTask의 기본 모델을 사용한다. */
    private Map<AiTask, AiModel> routing = new LinkedHashMap<>();

    private OpenAi openai = new OpenAi();
    private Gemini gemini = new Gemini();

    /** OpenAI 호환 Chat API 설정 (Cursor 등은 base-url만 교체). */
    public static class OpenAi {
        private String apiKey = "";
        private String baseUrl = "https://api.openai.com/v1";
        private String chatModel = "gpt-4o-mini";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /** Google Generative Language API 설정 (Gemini). */
    public static class Gemini {
        private String apiKey = "";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
        private String model = "gemini-2.5-flash-lite";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public boolean hasApiKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getUsdToKrw() {
        return usdToKrw;
    }

    public void setUsdToKrw(double usdToKrw) {
        this.usdToKrw = usdToKrw;
    }

    public double getMonthlyBudgetKrw() {
        return monthlyBudgetKrw;
    }

    public void setMonthlyBudgetKrw(double monthlyBudgetKrw) {
        this.monthlyBudgetKrw = monthlyBudgetKrw;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Map<AiTask, AiModel> getRouting() {
        return routing;
    }

    public void setRouting(Map<AiTask, AiModel> routing) {
        this.routing = routing;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public void setOpenai(OpenAi openai) {
        this.openai = openai;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public void setGemini(Gemini gemini) {
        this.gemini = gemini;
    }

    /** 작업에 적용할 모델: 라우팅 오버라이드 우선, 없으면 기본 모델. */
    public AiModel resolveModel(AiTask task) {
        return routing.getOrDefault(task, task.defaultModel());
    }
}
