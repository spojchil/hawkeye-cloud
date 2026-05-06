package com.hawkeye.detection.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 额外的组件扫描配置。
 * <p>
 * 显式扫描 {@code com.common.utils} 包，使公共模块中的组件得以注入。
 * <p>
 * 扫描的组件包括：
 * <ul>
 *   <li>MybatisMetaObjectHandler - 自动填充 create_time/update_time</li>
 *   <li>MultiTenantInterceptor - 多租户 SQL 拦截</li>
 *   <li>LogExecutionTimeAspect - 日志切面</li>
 *   <li>全局异常处理器</li>
 *   <li>RequestContextFilter - 请求上下文过滤器</li>
 * </ul>
 */
@Configuration
@ComponentScan(basePackages = "com.common.utils")
public class AdditionalScanConfig {
}
