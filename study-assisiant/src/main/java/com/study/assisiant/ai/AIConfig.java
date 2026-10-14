package com.study.assisiant.ai;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class AIConfig {


    @Value("${ai.api-key}")
    private String apiKey;


    @Value("${ai.url}")
    private String url;


    public String getApiKey(){
        return apiKey;
    }


    public String getUrl(){
        return url;
    }
}