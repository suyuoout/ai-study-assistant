package com.study.assisiant.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单的内存级每日限流：只针对使用「系统默认 Key」的调用计数，
 * 用户配置了自己的 Key 时不计数（花的是他自己的额度）。
 */
@Component
public class AiRateLimiter {

    /** 每用户每天允许调用次数；<=0 表示不限制 */
    @Value("${app.ai-daily-limit:50}")
    private int dailyLimit;

    private final Map<Long, Usage> usageMap = new ConcurrentHashMap<>();

    public void check(Long userId) {
        if (dailyLimit <= 0 || userId == null) {
            return;
        }

        LocalDate today = LocalDate.now();
        Usage usage = usageMap.computeIfAbsent(userId, k -> new Usage());

        synchronized (usage) {
            if (!today.equals(usage.date)) {
                usage.date = today;
                usage.count = 0;
            }
            if (usage.count >= dailyLimit) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "今日 AI 使用次数已达上限（" + dailyLimit
                                + " 次），可在个人中心配置自己的 API Key 后继续使用");
            }
            usage.count++;
        }
    }

    private static class Usage {
        private LocalDate date = LocalDate.now();
        private int count = 0;
    }
}
