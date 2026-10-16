package com.study.assisiant.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.study.assisiant.common.EnvNumber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录失败次数限制（内存实现，单实例部署足够）。
 *
 * <p>同一「账号 + IP」连续失败达到上限后锁定一段时间，
 * 避免密码被暴力枚举。多实例部署时应换成 Redis 计数。</p>
 */
@Component
public class LoginAttemptService {

    private static final Logger log =
            LoggerFactory.getLogger(LoginAttemptService.class);

    private final int maxAttempts;
    private final int lockMinutes;

    private final Map<String, Attempt> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${app.login-max-attempts:}") String maxAttempts,
            @Value("${app.login-lock-minutes:}") String lockMinutes) {
        this.maxAttempts = EnvNumber.toInt(maxAttempts, 5);
        this.lockMinutes = EnvNumber.toInt(lockMinutes, 10);
    }

    /** 登录前调用：被锁定则抛出 429 */
    public void checkBlocked(String username, String ip) {
        Attempt attempt = attempts.get(key(username, ip));
        if (attempt == null) {
            return;
        }
        synchronized (attempt) {
            if (attempt.lockedUntil != null
                    && attempt.lockedUntil.isAfter(LocalDateTime.now())) {
                long seconds = Duration.between(
                        LocalDateTime.now(), attempt.lockedUntil).toSeconds();
                log.warn("登录被锁定 username={} ip={} 剩余={}秒",
                        username, ip, seconds);
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "登录失败次数过多，请 " + Math.max(1, seconds / 60 + 1)
                                + " 分钟后再试");
            }
        }
    }

    public void onFailure(String username, String ip) {
        Attempt attempt = attempts.computeIfAbsent(
                key(username, ip), k -> new Attempt());
        synchronized (attempt) {
            attempt.count++;
            if (attempt.count >= maxAttempts) {
                attempt.lockedUntil = LocalDateTime.now()
                        .plusMinutes(lockMinutes);
                attempt.count = 0;
                log.warn("登录失败次数过多，锁定 username={} ip={} {}分钟",
                        username, ip, lockMinutes);
            }
        }
        sweepIfNeeded();
    }

    public void onSuccess(String username, String ip) {
        attempts.remove(key(username, ip));
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    private String key(String username, String ip) {
        return (username == null ? "" : username.toLowerCase()) + "|"
                + (ip == null ? "" : ip);
    }

    /** 定期清理过期条目，防止内存无限增长 */
    private void sweepIfNeeded() {
        if (attempts.size() < 1000) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        attempts.entrySet().removeIf(entry -> {
            Attempt attempt = entry.getValue();
            synchronized (attempt) {
                return attempt.lockedUntil == null
                        ? attempt.count == 0
                        : attempt.lockedUntil.isBefore(now);
            }
        });
    }

    private static class Attempt {
        private int count = 0;
        private LocalDateTime lockedUntil;
    }
}
