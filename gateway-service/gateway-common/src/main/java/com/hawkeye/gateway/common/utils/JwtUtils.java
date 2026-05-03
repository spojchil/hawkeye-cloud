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

@Slf4j
@Component
public class JwtUtils {

    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_USERNAME = "username";
    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret:hawkeye-cloud-secret-key-2026-must-be-at-least-256-bits}")
    private String secret;

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private SecretKey secretKey;

    public JwtUtils(ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException("JWT密钥长度不足");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseToken(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT Token已过期");
            return null;
        } catch (MalformedJwtException e) {
            log.warn("JWT Token格式错误");
            return null;
        } catch (Exception e) {
            log.warn("JWT Token解析失败: {}", e.getMessage());
            return null;
        }
    }

    public Mono<Boolean> validateToken(String token) {
        return isTokenInBlacklist(token)
                .flatMap(inBlacklist -> {
                    if (inBlacklist) return Mono.just(false);
                    return Mono.just(parseToken(token) != null);
                });
    }

    public boolean addTokenToBlacklist(String token) {
        Claims claims = parseToken(token);
        if (claims == null) return false;
        long remainingTime = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remainingTime <= 0) return true;
        String key = JWT_BLACKLIST_PREFIX + token;
        reactiveStringRedisTemplate.opsForValue()
                .set(key, "invalid", Duration.ofMillis(remainingTime))
                .subscribe();
        return true;
    }

    public Mono<Boolean> isTokenInBlacklist(String token) {
        if (token == null || token.isBlank()) return Mono.just(false);
        String key = JWT_BLACKLIST_PREFIX + token;
        return reactiveStringRedisTemplate.hasKey(key).map(Boolean.TRUE::equals);
    }

    public String getAccountIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    public Long getTenantIdFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get(CLAIM_TENANT_ID, Long.class) : null;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        return claims != null ? claims.get(CLAIM_USERNAME, String.class) : null;
    }
}
