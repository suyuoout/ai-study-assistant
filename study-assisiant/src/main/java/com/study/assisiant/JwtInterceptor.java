package com.study.assisiant;


import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;



@Component
public class JwtInterceptor implements HandlerInterceptor {


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


            return true;


        } catch (Exception e) {


            response.setStatus(401);

            return false;
        }
    }
}
