package com.hawkeye.detection.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 额外的组件扫描配置。
 * <p>
 * 显式扫描 {@code com.common.utils} 包，使公共模块中的 MybatisMetaObjectHandler、
 * MultiTenantInterceptor、LogExecutionTimeAspect、全局异常处理器等组件得以注入。
 */
@Configuration
@ComponentScan(basePackages = "com.common.utils")
public class AdditionalScanConfig {
}
