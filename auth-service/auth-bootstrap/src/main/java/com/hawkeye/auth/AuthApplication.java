package com.hawkeye.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

// ★ @ComponentScan("com.common.utils") 确保 auth-service 能扫到 common-utils 中的
//   MybatisPlusConfig（注册 TenantLineInnerInterceptor/PaginationInnerInterceptor）、
//   MultiTenantInterceptor（TenantLineHandler 实现）、MybatisMetaObjectHandler（自动填充）
@SpringBootApplication
@ComponentScan("com.common.utils")
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
