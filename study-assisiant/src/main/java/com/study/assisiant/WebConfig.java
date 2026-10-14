package com.study.assisiant;


import com.study.assisiant.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Autowired
    private JwtInterceptor jwtInterceptor;


    /**
     * 跨域环境变量白名单，多个用逗号分隔。
     * 前端由后端同源提供时不需要跨域，这里默认为空即关闭。
     */
    @Value("${app.allowed-origins:}")
    private String allowedOrigins;


    /**
     * 跨域配置：默认关闭。需要跨域时用 APP_ALLOWED_ORIGINS 显式配置白名单。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        if (allowedOrigins == null || allowedOrigins.isBlank()) {
            return;
        }

        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS"
                )
                .allowedHeaders("*")
                .allowCredentials(true);
    }


    /**
     * JWT拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {


        registry.addInterceptor(jwtInterceptor)

                .addPathPatterns("/api/**")

                .excludePathPatterns(
                        "/api/users/login",
                        "/api/users/register"
                );

    }

}
