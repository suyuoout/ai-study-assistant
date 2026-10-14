package com.study.assisiant;


import com.study.assisiant.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {


    @Autowired
    private JwtInterceptor jwtInterceptor;


    /**
     * 跨域配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173"
                )
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
