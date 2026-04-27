package com.hawkeye.asset.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * <p>
 * 为 Knife4j / Swagger UI 注册静态资源映射，使得 {@code /doc.html} 和 {@code /swagger-ui/**}
 * 能正确访问 Knife4j 的前端资源文件（位于 {@code classpath:/META-INF/resources/} 下）。
 * <p>
 * 如果不配置此映射，访问 Knife4j API 文档页面时会返回 404。
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Knife4j 增强文档页面，默认访问路径 /doc.html
        registry.addResourceHandler("/doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        // Swagger 原版 UI（兼容旧版）
        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        // Knife4j / Swagger 依赖的 webjars 静态资源（js/css）
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        // Swagger UI 3.x 的静态资源路径
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/swagger-ui/");
    }
}
