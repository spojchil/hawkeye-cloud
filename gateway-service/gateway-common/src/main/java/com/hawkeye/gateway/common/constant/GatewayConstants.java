package com.hawkeye.gateway.common.constant;

public interface GatewayConstants {

    String AUTHORIZATION_HEADER = "Authorization";
    String BEARER_PREFIX = "Bearer ";

    /** 网关注入下游的请求头 — 与 com.common.utils.constant.HeaderConstants 保持一致 */
    String ACCOUNT_ID_HEADER = "X-ACCOUNT-ID";
    String TENANT_ID_HEADER = "X-TENANT-ID";
    String USERNAME_HEADER = "X-USERNAME";

    /**
     * 白名单：路径带 /api 前缀（AuthFilter order=0，在 StripPrefix 之前执行）。
     */
    String[] WHITE_LIST = {
            "/api/auth/login",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**"
    };
}
