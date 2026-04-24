package com.hawkeye.gateway.common.constant;

/**
 * 网关常量定义
 */
public interface GatewayConstants {
    /**
     * JWT请求头
     */
    String AUTHORIZATION_HEADER = "Authorization";
    String BEARER_PREFIX = "Bearer ";

    String ACCOUNT_ID_HEADER = "account-id";       // 账号ID
    String TENANT_ID_HEADER = "tenant-id";   // 租户ID

    /**
     * 白名单
     */
    String[] WHITE_LIST = {
            "/auth/login",       // 登录接口
            "/auth/register",    // 注册接口
            "/doc.html",         // swagger
            "/webjars/**",
            "/v3/api-docs/**"
    };
}