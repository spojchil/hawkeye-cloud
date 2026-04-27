package com.hawkeye.gateway.business.fillter;

import com.hawkeye.gateway.common.constant.GatewayConstants;
import com.hawkeye.gateway.common.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 网关认证过滤器
 * 1. 白名单放行
 * 2. 校验JWT Token
 * 3. 解析用户ID/租户ID写入请求头
 * 4. 无效Token返回401
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().toString();

        // 1. 白名单直接放行
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        // 2. 获取请求头中的Token
        String token = getToken(request);
        if (token == null) {
            return unauthorizedResponse(exchange, "Token不能为空");
        }

        // 3. 校验Token（黑名单+合法性+过期）
        //    使用 flatMap 响应式链式处理，避免 block() 阻塞 Netty reactor 线程
        return jwtUtils.validateToken(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return unauthorizedResponse(exchange, "Token无效或已失效");
                    }

                    // 4. 解析账号ID、租户ID
                    String accountId = jwtUtils.getAccountIdFromToken(token);
                    Long tenantId = jwtUtils.getTenantIdFromToken(token);

                    // 5. 将用户信息写入请求头，转发给下游微服务
                    ServerHttpRequest.Builder requestBuilder = request.mutate()
                            .header(GatewayConstants.ACCOUNT_ID_HEADER, accountId)
                            .header(GatewayConstants.TENANT_ID_HEADER, String.valueOf(tenantId));

                    // 6. 放行请求
                    return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
                });
    }

    /**
     * 判断是否白名单
     */
    private boolean isWhiteList(String path) {
        for (String whitePath : GatewayConstants.WHITE_LIST) {
            if (antPathMatcher.match(whitePath, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从请求头提取Token
     */
    private String getToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(GatewayConstants.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(GatewayConstants.BEARER_PREFIX)) {
            // 截取 Bearer 后的真实Token
            return header.substring(GatewayConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 统一401未授权响应
     */
    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String msg) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 401);
        result.put("message", msg);
        result.put("success", false);

        String json = new com.alibaba.fastjson2.JSONObject(result).toString();
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    /**
     * 过滤器执行顺序（数值越小优先级越高）
     */
    @Override
    public int getOrder() {
        return 0;
    }
}