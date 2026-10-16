package com.study.assisiant.ai;

import com.study.assisiant.common.EnvNumber;
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

    @Value("${ai.temperature:}")
    private String temperature;

    @Value("${ai.max-tokens:}")
    private String maxTokens;

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
        return EnvNumber.toDouble(temperature, 0.7);
    }

    public int getMaxTokens() {
        return EnvNumber.toInt(maxTokens, 8192);
    }

    public boolean hasSystemKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
