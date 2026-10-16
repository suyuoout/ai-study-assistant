package com.study.assisiant.auth;

import com.study.assisiant.common.CryptoService;
import com.study.assisiant.mapper.SysUserMapper;
import com.study.assisiant.user.SysUser;
import com.study.assisiant.user.UserCache;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 登录校验拦截器。
 *
 * <p>除了校验令牌，还负责把「当前用户」放进请求上下文：
 * 角色取自数据库（不是令牌声明），禁用账号直接拒绝，
 * 并顺带解出该用户自己的 AI Key。</p>
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER_ID = "userId";
    public static final String ATTR_ROLE = "role";
    public static final String ATTR_AI_KEY = "aiApiKey";
    public static final String ATTR_USERNAME = "username";

    private final SysUserMapper sysUserMapper;
    private final UserCache userCache;
    private final CryptoService cryptoService;

    public JwtInterceptor(SysUserMapper sysUserMapper,
                          UserCache userCache,
                          CryptoService cryptoService) {
        this.sysUserMapper = sysUserMapper;
        this.userCache = userCache;
        this.cryptoService = cryptoService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return reject(response, 401, "未登录或登录已失效，请重新登录");
        }

        Long userId;
        try {
            Claims claims = JwtUtil.parseToken(token.substring(7));
            userId = Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            return reject(response, 401, "登录状态已过期，请重新登录");
        }

        SysUser user = userCache.get(userId,
                () -> sysUserMapper.findById(userId));
        if (user == null) {
            return reject(response, 401, "账号不存在，请重新登录");
        }
        if (user.getEnabled() != null && !user.getEnabled()) {
            return reject(response, 403, "账号已被禁用，请联系管理员");
        }

        request.setAttribute(ATTR_USER_ID, userId);
        request.setAttribute(ATTR_USERNAME, user.getUsername());
        request.setAttribute(ATTR_ROLE,
                user.getRole() == null ? "USER" : user.getRole());

        if (user.getAiApiKey() != null && !user.getAiApiKey().isBlank()) {
            String key = cryptoService.decrypt(user.getAiApiKey());
            if (key != null && !key.isBlank()) {
                request.setAttribute(ATTR_AI_KEY, key);
            }
        }

        return true;
    }

    /** 以 JSON 形式返回错误，前端才能拿到可读提示 */
    private boolean reject(HttpServletResponse response, int status,
                           String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = "{\"code\":" + status + ",\"message\":\""
                + message + "\",\"data\":null}";
        response.getOutputStream().write(
                body.getBytes(StandardCharsets.UTF_8));
        return false;
    }
}
