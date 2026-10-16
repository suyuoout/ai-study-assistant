package com.study.assisiant.ai;

import com.study.assisiant.auth.JwtInterceptor;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 一次 AI 调用的上下文：谁在调用、用谁的 Key、算什么用途。
 *
 * <p>之所以显式传递而不是用 ThreadLocal：
 * 流式响应（SSE）会在异步线程里写数据，那时请求上下文已经拿不到了。</p>
 */
public record AiCallContext(Long userId, String userKey, String kind) {

    public static AiCallContext of(HttpServletRequest request, String kind) {
        Object userId = request.getAttribute(JwtInterceptor.ATTR_USER_ID);
        Object userKey = request.getAttribute(JwtInterceptor.ATTR_AI_KEY);
        return new AiCallContext(
                userId instanceof Long id ? id : null,
                userKey == null ? null : String.valueOf(userKey),
                kind);
    }

    public boolean hasUserKey() {
        return userKey != null && !userKey.isBlank();
    }
}
