package com.hawkeye.gateway;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Gateway启动类
 */
@SpringBootApplication
// @EnableDiscoveryClient 无需显示声明服务
public class GatewayApplication {
    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(GatewayApplication.class, args);
    }
}
