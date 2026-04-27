package com.hawkeye.gateway.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 网关JWT工具类
 * 1. 修复原有逻辑漏洞
 * 2. 响应式Redis黑名单实现Token主动失效
 * 3. 高并发适配、异常细分、常量抽离
 */
@Slf4j
@Component
public class JwtUtils {

    // ==================== 常量定义 ====================
    /** JWT存储租户ID */
    public static final String CLAIM_TENANT_ID = "tenantId";
    /** JWT存储用户ID */
    public static final String CLAIM_USER_ID = "userId";
    /** Redis黑名单前缀 */
    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";
    /** HMAC-SHA256 密钥最小长度（32字节 = 256位） */
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret:hawkeye-cloud-secret-key-2026-must-be-at-least-256-bits}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    // 响应式RedisTemplate
    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private SecretKey secretKey;

    // 构造器注入
    public JwtUtils(ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    /**
     * 初始化：校验密钥长度并生成密钥对象
     */
    @PostConstruct
    public void init() {
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException("JWT密钥长度不足，必须≥32个字符（256位）");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析Token
     * @return Claims 未过期、合法的载荷；null=解析失败
     */
    public Claims parseToken(String token) {
        // 前置非空校验
        if (token == null || token.isBlank()) {
            log.warn("JWT Token为空，无法解析");
            return null;
        }

        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.error("JWT Token已过期");
            return null;
        } catch (MalformedJwtException e) {
            log.error("JWT Token格式错误");
            return null;
        } catch (SecurityException e) {
            log.error("JWT Token签名/密钥错误");
            return null;
        } catch (Exception e) {
            log.error("JWT Token解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验Token有效性（黑名单 + 合法性 + 过期时间）
     * 返回 Mono 而非 boolean：Gateway 跑在 Netty reactor 线程上，Redis 黑名单查询是
     * 异步的，.block() 会导致 "block() are blocking" IllegalStateException。
     */
    public Mono<Boolean> validateToken(String token) {
        return isTokenInBlacklist(token)
                .flatMap(inBlacklist -> {
                    if (inBlacklist) {
                        log.warn("JWT Token已被加入黑名单，强制失效");
                        return Mono.just(false);
                    }
                    return Mono.just(parseToken(token) != null);
                });
    }

    /**
     * 将Token加入黑名单（实现主动失效：登出/修改密码/强制踢人）
     * @param token 要失效的Token
     * @return 操作结果
     */
    public boolean addTokenToBlacklist(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            log.warn("无效Token，无需加入黑名单");
            return false;
        }

        // 剩余过期时间
        long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remainingTime <= 0) {
            return true;
        }

        String key = JWT_BLACKLIST_PREFIX + token;
        // 响应式Redis存入，设置自动过期（和Token剩余时间一致）
        reactiveStringRedisTemplate.opsForValue()
                .set(key, "invalid", Duration.ofMillis(remainingTime))
                .subscribe();

        log.info("Token已加入黑名单，剩余有效时间：{}ms", remainingTime);
        return true;
    }

    /**
     * 检查Token是否在黑名单中（响应式，不阻塞 Netty 事件循环）
     */
    public Mono<Boolean> isTokenInBlacklist(String token) {
        if (token == null || token.isBlank()) {
            return Mono.just(false);
        }
        String key = JWT_BLACKLIST_PREFIX + token;
        return reactiveStringRedisTemplate.hasKey(key).map(Boolean.TRUE::equals);
    }

    /**
     * 获取账号ID（subject）
     */
    public String getAccountIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 获取租户ID
     */
    public Long getTenantIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get(CLAIM_TENANT_ID, Long.class) : null;
    }

//  认证服务的生成Token方法
//    public JwtToken generateToken(Long accountId, Long tenantId) {
//        Date now = new Date();
//        Date expiryDate = new Date(now.getTime() + expiration);
//
//        String token = Jwts.builder()
//                .claim(CLAIM_TENANT_ID, tenantId)
//                .subject(String.valueOf(accountId))
//                .issuedAt(now)
//                .expiration(expiryDate)
//                .signWith(secretKey)
//                .compact();
//
//        return new JwtToken(token);
//    }
}