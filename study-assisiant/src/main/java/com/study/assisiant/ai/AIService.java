package com.study.assisiant.ai;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AIService {

    private static final Logger log =
            LoggerFactory.getLogger(AIService.class);

    @Autowired
    private AIConfig aiConfig;

    public String generatePlan(AIRequest request) {
        String prompt = buildPrompt(request);

        JSONArray messages = new JSONArray();
        messages.add(userMessage(prompt));

        return callDeepSeek(messages);
    }

    /**
     * 多轮对话：messages 元素为 {role: "user"/"assistant", content: "..."}
     */
    public String chat(List<Map<String, String>> messages) {
        JSONArray array = new JSONArray();
        for (Map<String, String> m : messages) {
            String role = "assistant".equals(m.get("role"))
                    ? "assistant" : "user";
            array.add(roleMessage(role, m.get("content")));
        }
        return callDeepSeek(array);
    }

    /**
     * 带系统提示词的补全调用
     */
    public String chatWithSystem(String systemPrompt, String userPrompt) {
        JSONArray array = new JSONArray();
        array.add(roleMessage("system", systemPrompt));
        array.add(roleMessage("user", userPrompt));
        return callDeepSeek(array);
    }

    private JSONObject userMessage(String content) {
        return roleMessage("user", content);
    }

    private JSONObject roleMessage(String role, String content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String buildPrompt(AIRequest request) {
        return """
                你是一名Java后端导师。

                学习目标：
                %s

                当前水平：
                %s

                学习周期：
                %d天

                每日时间：
                %d小时

                请生成详细学习计划。
                """.formatted(
                request.getGoal(),
                request.getLevel(),
                request.getDays(),
                request.getHours()
        );
    }

    private String callDeepSeek(JSONArray messages) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        JSONObject bodyJson = new JSONObject();
        bodyJson.put("model", "deepseek-chat");
        bodyJson.put("messages", messages);

        RequestBody body = RequestBody.create(
                bodyJson.toJSONString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(aiConfig.getUrl())
                .addHeader(
                        "Authorization",
                        "Bearer " + aiConfig.getApiKey()
                )
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String result = response.body().string();
            JSONObject json = JSONObject.parseObject(result);
            JSONArray choices = json.getJSONArray("choices");
            JSONObject first = choices.getJSONObject(0);
            return first.getJSONObject("message").getString("content");
        } catch (Exception e) {
            log.error("调用 DeepSeek 接口失败", e);
            throw new RuntimeException("AI生成失败，请稍后重试", e);
        }
    }
}
