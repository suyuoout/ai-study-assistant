
package com.study.assisiant;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // JWT 密钥：从配置读取，正式部署时请通过环境变量 JWT_SECRET 注入
    private static String SECRET;

    // 令牌有效期：2 小时
    private static final long EXPIRE_TIME = 2 * 60 * 60 * 1000L;

    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        SECRET = secret;
    }

    private static SecretKey key() {
        if (SECRET == null || SECRET.isBlank()) {
            throw new IllegalStateException(
                    "JWT 密钥未配置，请设置 jwt.secret");
        }
        return Keys.hmacShaKeyFor(
                SECRET.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 根据用户 ID、用户名和角色生成 JWT
     */
    public static String generateToken(
            Long userId, String username, String role) {
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + EXPIRE_TIME);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(key())
                .compact();
    }

    /**
     * 解析并验证 JWT
     */
    public static Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
