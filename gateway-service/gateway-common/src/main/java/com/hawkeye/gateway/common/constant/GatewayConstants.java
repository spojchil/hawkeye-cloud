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
     * 白名单：路径必须带 /api 前缀，因为 AuthFilter（order=0）在 StripPrefix（order=1）
     * 之前执行，此时看到的请求路径尚未去除 /api 前缀。
     */
    String[] WHITE_LIST = {
            "/api/auth/login",       // 登录接口
            "/api/auth/register",    // 注册接口
            "/doc.html",         // swagger
            "/webjars/**",
            "/v3/api-docs/**"
    };
}