package com.study.assisiant.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AIConfig {

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.url:https://api.deepseek.com/chat/completions}")
    private String url;

    @Value("${ai.model:deepseek-chat}")
    private String model;

    @Value("${ai.temperature:0.7}")
    private double temperature;

    @Value("${ai.max-tokens:2048}")
    private int maxTokens;

    public String getApiKey() {
        return apiKey;
    }

    public String getUrl() {
        return url;
    }

    public String getModel() {
        return model;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public boolean hasSystemKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
