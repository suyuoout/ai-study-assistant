package com.study.assisiant;


import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.study.assisiant.mapper.SysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;



@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private SysUserMapper sysUserMapper;


    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {


        // OPTIONS请求放行（解决跨域）
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }


        // 获取请求头token
        String token = request.getHeader("Authorization");


        if (token == null || !token.startsWith("Bearer ")) {

            response.setStatus(401);
            return false;
        }


        token = token.substring(7);


        try {

            Claims claims = JwtUtil.parseToken(token);


            // 获取用户id
            Long userId =
                    Long.valueOf(claims.getSubject());


            // 保存当前用户ID
            request.setAttribute(
                    "userId",
                    userId
            );

            request.setAttribute(
                    "role",
                    claims.get("role")
            );

            // 该用户自己配置的 DeepSeek Key（可能为 null）
            SysUser user = sysUserMapper.findById(userId);
            if (user != null
                    && user.getAiApiKey() != null
                    && !user.getAiApiKey().isBlank()) {
                request.setAttribute("aiApiKey", user.getAiApiKey());
            }


            return true;


        } catch (Exception e) {


            response.setStatus(401);

            return false;
        }
    }
}
