package com.hawkeye.asset.common.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 额外的组件扫描配置
 */
@Configuration
@ComponentScan(basePackages = "com.common.utils")
public class AdditionalScanConfig {
}
