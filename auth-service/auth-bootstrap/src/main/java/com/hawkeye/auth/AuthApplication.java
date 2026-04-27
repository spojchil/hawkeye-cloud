package com.hawkeye.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

// ★ @ComponentScan 需同时包含两个包路径，因为显式指定后会覆盖 @SpringBootApplication 的默认扫描：
//   1. "com.common.utils" → common-utils 中的 MybatisPlusConfig、MultiTenantInterceptor、MybatisMetaObjectHandler 等
//   2. "com.hawkeye.auth"  → auth-service 自身的 Controller、Service、Mapper、Config 等
//   如果只写 "com.common.utils" 会导致所有 com.hawkeye.auth 下的 Bean 不被注册，请求返回 "资源不存在"
@SpringBootApplication
@ComponentScan({"com.common.utils", "com.hawkeye.auth"})
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
