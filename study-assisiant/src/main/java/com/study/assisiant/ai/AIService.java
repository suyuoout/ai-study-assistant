package com.study.assisiant.ai;

import com.study.assisiant.common.EnvNumber;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * DeepSeek（OpenAI 风格）接口封装。
 *
 * <p>要点：
 * <ul>
 *   <li>OkHttpClient 是单例，复用连接池与线程池；</li>
 *   <li>非 2xx 响应会把上游错误信息带出来，而不是笼统的「AI 生成失败」；</li>
 *   <li>每次调用都写入 ai_usage，用于额度限制与后台统计；</li>
 *   <li>支持流式（SSE）输出。</li>
 * </ul>
 */
@Service
public class AIService {

    private static final Logger log =
            LoggerFactory.getLogger(AIService.class);

    private static final MediaType JSON_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private static final int MAX_ERROR_BODY = 400;

    private final OkHttpClient client;
    private final AIConfig aiConfig;
    private final AiRateLimiter aiRateLimiter;
    private final AiUsageService usageService;

    public AIService(
            AIConfig aiConfig,
            AiRateLimiter aiRateLimiter,
            AiUsageService usageService,
            @Value("${ai.connect-timeout-seconds:}") String connectTimeout,
            @Value("${ai.read-timeout-seconds:}") String readTimeout) {

        this.aiConfig = aiConfig;
        this.aiRateLimiter = aiRateLimiter;
        this.usageService = usageService;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(EnvNumber.toLong(connectTimeout, 30),
                        TimeUnit.SECONDS)
                .readTimeout(EnvNumber.toLong(readTimeout, 120),
                        TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(20, 5, TimeUnit.MINUTES))
                .build();
    }

    /** 多轮对话 */
    public String chat(List<Map<String, String>> messages,
                       AiCallContext ctx) {
        JSONArray array = new JSONArray();
        for (Map<String, String> message : messages) {
            String role = "assistant".equals(message.get("role"))
                    ? "assistant" : "user";
            array.add(roleMessage(role, message.get("content")));
        }
        return complete(array, ctx, false);
    }

    /** 带系统提示词的补全 */
    public String chatWithSystem(String systemPrompt, String userPrompt,
                                 AiCallContext ctx) {
        return complete(messages(systemPrompt, userPrompt), ctx, false);
    }

    /** 要求模型输出 JSON 对象（学习计划等结构化场景） */
    public String chatJson(String systemPrompt, String userPrompt,
                           AiCallContext ctx) {
        return complete(messages(systemPrompt, userPrompt), ctx, true);
    }

    /**
     * 流式对话：每收到一段增量文本就回调一次 onDelta。
     *
     * @return 完整的回答文本
     */
    public String chatStream(List<Map<String, String>> messages,
                             AiCallContext ctx,
                             Consumer<String> onDelta) {

        String apiKey = effectiveApiKey(ctx);
        JSONObject body = buildBody(toMessages(messages), true, false);
        Request request = newRequest(body, apiKey);

        StringBuilder full = new StringBuilder();
        boolean ownKey = ctx.hasUserKey();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String detail = errorDetail(response);
                usageService.record(ctx.userId(), ctx.kind(),
                        aiConfig.getModel(), null, null, ownKey, false);
                log.error("DeepSeek 流式调用失败 status={} body={}",
                        response.code(), detail);
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI 服务返回错误（" + response.code() + "）：" + detail);
            }

            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "AI 服务返回空响应");
            }

            BufferedSource source = responseBody.source();
            String line;
            while ((line = source.readUtf8Line()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring(5).trim();
                if (payload.isEmpty() || "[DONE]".equals(payload)) {
                    continue;
                }
                String delta = deltaOf(payload);
                if (delta != null && !delta.isEmpty()) {
                    full.append(delta);
                    if (onDelta != null) {
                        onDelta.accept(delta);
                    }
                }
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            usageService.record(ctx.userId(), ctx.kind(),
                    aiConfig.getModel(), null, null, ownKey, false);
            log.error("DeepSeek 流式调用异常", e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "AI 服务连接失败，请稍后重试");
        }

        usageService.record(ctx.userId(), ctx.kind(),
                aiConfig.getModel(), null, null, ownKey, true);
        return full.toString();
    }

    // ------------------------------------------------------------------
    // 内部实现
    // ------------------------------------------------------------------

    private String complete(JSONArray messages, AiCallContext ctx,
                            boolean jsonMode) {
        String apiKey = effectiveApiKey(ctx);
        boolean ownKey = ctx.hasUserKey();

        JSONObject body = buildBody(messages, false, jsonMode);
        Request request = newRequest(body, apiKey);

        try (Response response = client.newCall(request).execute()) {
            String raw = response.body() == null
                    ? "" : response.body().string();

            if (!response.isSuccessful()) {
                String detail = extractError(raw, response.code());
                usageService.record(ctx.userId(), ctx.kind(),
                        aiConfig.getModel(), null, null, ownKey, false);
                log.error("DeepSeek 调用失败 status={} body={}",
                        response.code(), detail);
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "AI 服务返回错误（" + response.code() + "）：" + detail);
            }

            JSONObject json = JSONObject.parseObject(raw);
            JSONArray choices = json == null
                    ? null : json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                usageService.record(ctx.userId(), ctx.kind(),
                        aiConfig.getModel(), null, null, ownKey, false);
                log.error("DeepSeek 返回内容为空: {}", truncate(raw));
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "AI 返回内容为空，请稍后重试");
            }

            Integer promptTokens = null;
            Integer completionTokens = null;
            JSONObject usage = json.getJSONObject("usage");
            if (usage != null) {
                promptTokens = usage.getInteger("prompt_tokens");
                completionTokens = usage.getInteger("completion_tokens");
            }
            usageService.record(ctx.userId(), ctx.kind(),
                    aiConfig.getModel(), promptTokens, completionTokens,
                    ownKey, true);

            JSONObject first = choices.getJSONObject(0);
            String finishReason = first == null
                    ? null : first.getString("finish_reason");
            if (jsonMode && "length".equals(finishReason)) {
                // 结构化输出被截断时 JSON 必然不完整，直接给出可操作的提示
                log.warn("AI 结构化输出被截断 model={} maxTokens={}",
                        aiConfig.getModel(), aiConfig.getMaxTokens());
                usageService.record(ctx.userId(), ctx.kind(),
                        aiConfig.getModel(), promptTokens, completionTokens,
                        ownKey, false);
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI 输出被截断：计划内容超出了单次生成上限，"
                                + "请缩短时间范围（如 30 天）或减少每日任务量后重试");
            }
            JSONObject message = first == null
                    ? null : first.getJSONObject("message");
            String content = message == null
                    ? null : message.getString("content");
            if (content == null || content.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "AI 返回内容为空，请稍后重试");
            }
            return content;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (IOException e) {
            usageService.record(ctx.userId(), ctx.kind(),
                    aiConfig.getModel(), null, null, ownKey, false);
            log.error("DeepSeek 调用异常", e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "AI 服务连接失败，请稍后重试");
        }
    }

    private JSONArray messages(String systemPrompt, String userPrompt) {
        JSONArray array = new JSONArray();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            array.add(roleMessage("system", systemPrompt));
        }
        array.add(roleMessage("user", userPrompt));
        return array;
    }

    private JSONArray toMessages(List<Map<String, String>> messages) {
        JSONArray array = new JSONArray();
        for (Map<String, String> message : messages) {
            String role = "assistant".equals(message.get("role"))
                    ? "assistant" : "user";
            array.add(roleMessage(role, message.get("content")));
        }
        return array;
    }

    private JSONObject roleMessage(String role, String content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private JSONObject buildBody(JSONArray messages, boolean stream,
                                 boolean jsonMode) {
        JSONObject body = new JSONObject();
        body.put("model", aiConfig.getModel());
        body.put("messages", messages);
        body.put("temperature", aiConfig.getTemperature());
        body.put("max_tokens", aiConfig.getMaxTokens());
        if (stream) {
            body.put("stream", true);
        }
        if (jsonMode) {
            JSONObject format = new JSONObject();
            format.put("type", "json_object");
            body.put("response_format", format);
        }
        return body;
    }

    private Request newRequest(JSONObject body, String apiKey) {
        RequestBody requestBody = RequestBody.create(
                body.toJSONString(), JSON_TYPE);
        return new Request.Builder()
                .url(aiConfig.getUrl())
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(requestBody)
                .build();
    }

    /**
     * 选择使用哪个 Key：优先用户自己的（不计系统额度），
     * 否则用系统 Key 并按日限流。
     */
    private String effectiveApiKey(AiCallContext ctx) {
        if (ctx.hasUserKey()) {
            return ctx.userKey();
        }
        if (!aiConfig.hasSystemKey()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "系统未配置 AI Key，请在「个人中心」填写自己的 DeepSeek Key 后使用");
        }
        aiRateLimiter.check(ctx.userId());
        return aiConfig.getApiKey();
    }

    /** 从流式分片里取出增量文本 */
    private String deltaOf(String payload) {
        try {
            JSONObject json = JSONObject.parseObject(payload);
            JSONArray choices = json == null
                    ? null : json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JSONObject delta = choices.getJSONObject(0).getJSONObject("delta");
            return delta == null ? null : delta.getString("content");
        } catch (Exception e) {
            log.debug("忽略无法解析的流式分片: {}", truncate(payload));
            return null;
        }
    }

    /** 解析上游错误体，尽量给出可读原因 */
    private String errorDetail(Response response) {
        try {
            String raw = response.body() == null
                    ? "" : response.body().string();
            return extractError(raw, response.code());
        } catch (Exception e) {
            return "HTTP " + response.code();
        }
    }

    private String extractError(String raw, int status) {
        if (raw == null || raw.isBlank()) {
            return "HTTP " + status;
        }
        try {
            JSONObject json = JSONObject.parseObject(raw);
            JSONObject error = json == null
                    ? null : json.getJSONObject("error");
            if (error != null && error.getString("message") != null) {
                return truncate(error.getString("message"));
            }
        } catch (Exception ignored) {
            // 非 JSON 错误体，直接截断返回
        }
        return truncate(raw);
    }

    private String truncate(String text) {
        if (text == null) {
            return "";
        }
        String single = text.replaceAll("\\s+", " ").trim();
        return single.length() > MAX_ERROR_BODY
                ? single.substring(0, MAX_ERROR_BODY) + "..."
                : single;
    }
}
