package com.keke.shared.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 
 * - 配置跨域支持
 * - 配置静态资源
 * - 配置默认首页
 * - SPA路由支持
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // SPA路由支持：将前端路由转发到index.html
        registry.addViewController("/").setViewName("forward:/index.html");
        registry.addViewController("/cases").setViewName("forward:/index.html");
        registry.addViewController("/cases/**").setViewName("forward:/index.html");
        registry.addViewController("/todo").setViewName("forward:/index.html");
        registry.addViewController("/todo/**").setViewName("forward:/index.html");
        registry.addViewController("/logs").setViewName("forward:/index.html");
        registry.addViewController("/logs/**").setViewName("forward:/index.html");
        registry.addViewController("/settings").setViewName("forward:/index.html");
        registry.addViewController("/settings/**").setViewName("forward:/index.html");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
