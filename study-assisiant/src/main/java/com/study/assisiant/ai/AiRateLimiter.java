package com.study.assisiant.ai;

import com.study.assisiant.common.EnvNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 每日额度限制：只针对使用「系统默认 Key」的调用计数。
 *
 * <p>计数来源是 ai_usage 表，而不是内存计数器，
 * 因此重启服务、多实例部署都不会把额度算错。</p>
 */
@Component
public class AiRateLimiter {

    @Value("${app.ai-daily-limit:}")
    private String dailyLimit;

    private final AiUsageService usageService;

    public AiRateLimiter(AiUsageService usageService) {
        this.usageService = usageService;
    }

    public void check(Long userId) {
        int limit = getDailyLimit();
        if (limit <= 0 || userId == null) {
            return;
        }
        long used = usageService.countTodaySystemKey(userId);
        if (used >= limit) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "今日 AI 使用次数已达上限（" + limit
                            + " 次），可在个人中心配置自己的 API Key 后继续使用");
        }
    }

    public int getDailyLimit() {
        return EnvNumber.toInt(dailyLimit, 50);
    }

    public long usedToday(Long userId) {
        return userId == null ? 0
                : usageService.countTodaySystemKey(userId);
    }
}
