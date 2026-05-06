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

        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        String token = getToken(request);
        if (token == null) {
            return unauthorizedResponse(exchange, "Token不能为空");
        }

        return jwtUtils.validateToken(token)
                .flatMap(isValid -> {
                    if (!isValid) {
                        return unauthorizedResponse(exchange, "Token无效或已失效");
                    }

                    String accountId = jwtUtils.getAccountIdFromToken(token);
                    String username = jwtUtils.getUsernameFromToken(token);
                    Long tenantId = jwtUtils.getTenantIdFromToken(token);

                    ServerHttpRequest.Builder requestBuilder = request.mutate()
                            .header(GatewayConstants.ACCOUNT_ID_HEADER, accountId)
                            .header(GatewayConstants.USERNAME_HEADER, username)
                            .header(GatewayConstants.TENANT_ID_HEADER, String.valueOf(tenantId));

                    return chain.filter(exchange.mutate().request(requestBuilder.build()).build());
                });
    }

    private boolean isWhiteList(String path) {
        for (String whitePath : GatewayConstants.WHITE_LIST) {
            if (antPathMatcher.match(whitePath, path)) {
                return true;
            }
        }
        return false;
    }

    private String getToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(GatewayConstants.AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(GatewayConstants.BEARER_PREFIX)) {
            return header.substring(GatewayConstants.BEARER_PREFIX.length());
        }
        return null;
    }

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

    @Override
    public int getOrder() {
        return 0;
    }
}
