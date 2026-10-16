package com.study.assisiant.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 生成与校验。
 *
 * <p>令牌里只保存「身份」（用户 ID、用户名），角色等权限信息
 * 一律在服务端按用户 ID 实时查询，避免降权后旧令牌仍然有效。</p>
 */
@Component
public class JwtUtil {

    private static String SECRET;

    /** 令牌有效期，默认 2 小时，可通过 jwt.expire-minutes 调整 */
    private static long expireMillis = 2 * 60 * 60 * 1000L;

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        SECRET = secret;
    }

    @Value("${jwt.expire-minutes:120}")
    public void setExpireMinutes(long minutes) {
        if (minutes > 0) {
            expireMillis = minutes * 60_000L;
        }
    }

    public static long getExpireSeconds() {
        return expireMillis / 1000;
    }

    private static SecretKey key() {
        if (SECRET == null || SECRET.isBlank()) {
            throw new IllegalStateException(
                    "JWT 密钥未配置，请设置 jwt.secret");
        }
        return Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(
            Long userId, String username, String role) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expireMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                // 保留 role 便于前端展示；服务端鉴权一律以数据库为准
                .claim("role", role)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key())
                .compact();
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
