package com.study.assisiant.user;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 用户短缓存：拦截器每个请求都要取用户（角色、启用状态、个人 Key），
 * 直接用数据库会造成每次 API 调用多一次查询。
 *
 * <p>缓存默认 60 秒，角色变更 / 禁用 / 改 Key 时会主动失效，
 * 因此权限变动的生效延迟上限就是 TTL。</p>
 */
@Component
public class UserCache {

    private final long ttlSeconds;

    private final Map<Long, Entry> cache = new ConcurrentHashMap<>();

    public UserCache(@Value("${app.user-cache-seconds:60}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public SysUser get(Long userId, Supplier<SysUser> loader) {
        if (userId == null || ttlSeconds <= 0) {
            return loader.get();
        }
        Entry entry = cache.get(userId);
        if (entry != null && !entry.expired(ttlSeconds)) {
            return entry.user;
        }
        SysUser user = loader.get();
        if (user == null) {
            cache.remove(userId);
            return null;
        }
        cache.put(userId, new Entry(user, LocalDateTime.now()));
        return user;
    }

    public void evict(Long userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    public void evictAll() {
        cache.clear();
    }

    private static class Entry {
        private final SysUser user;
        private final LocalDateTime loadedAt;

        private Entry(SysUser user, LocalDateTime loadedAt) {
            this.user = user;
            this.loadedAt = loadedAt;
        }

        private boolean expired(long ttlSeconds) {
            return loadedAt.plusSeconds(ttlSeconds)
                    .isBefore(LocalDateTime.now());
        }
    }
}
