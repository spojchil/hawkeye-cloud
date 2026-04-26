package com.hawkeye.asset.common.config;

import com.common.utils.filter.RequestContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义组件扫描配置类
 */
@Configuration
@ComponentScan(basePackages = "com.common.utils")
public class AdditionalScanConfig {

    @Bean
    public FilterRegistrationBean<RequestContextFilter> requestContextFilterRegistration() {
        // 创建过滤器注册Bean
        FilterRegistrationBean<RequestContextFilter> registration = new FilterRegistrationBean<>();

        // 设置过滤器实例
        registration.setFilter(new RequestContextFilter());

        // 设置URL匹配模式：匹配所有请求
        registration.addUrlPatterns("/*");

        // 设置过滤器名称，用于日志和监控
        registration.setName("requestContextFilter");

        // 设置执行顺序：数字越小优先级越高
        // Order=1 表示最高优先级之一，通常用于设置请求上下文
        registration.setOrder(1);
        return registration;
    }
}
