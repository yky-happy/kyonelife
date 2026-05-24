package com.yky.blog.common.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()) {
                    @Override
                    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                        if ("OPTIONS".equals(request.getMethod())) return true;
                        return super.preHandle(request, response, handler);
                    }
                })
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/admin/auth/login",
                        "/api/**",
                        "/doc.html",
                        "/webjars/**",
                        "/swagger-resources/**",
                        "/v3/api-docs",
                        "/v3/api-docs/**"
                );
    }
}
