package com.yky.blog.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("kyonelife 博客接口文档")
                .description("博客系统后台管理接口")
                .version("1.0.0")
                .contact(new Contact().name("yky").email("heaamazing@gmail.com")));
    }
}
