package com.hawkeye.asset.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 额外的组件扫描配置
 * <p>
 * Spring Boot 默认只扫描启动类所在包及其子包（即 {@code com.hawkeye.asset}），
 * 但公共模块 {@code com.common.utils} 中的全局组件（如过滤器、切面、异常处理器等）
 * 不在自动扫描范围内。通过此配置显式扫描 {@code com.common.utils} 包，
 * 使这些公共组件得以注入到 Spring 容器中。
 */
@Configuration
@ComponentScan(basePackages = "com.common.utils")
public class AdditionalScanConfig {
}
