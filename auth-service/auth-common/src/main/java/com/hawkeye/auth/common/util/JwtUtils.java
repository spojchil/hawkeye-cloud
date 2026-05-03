package com.hawkeye.auth.common.util;

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

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

@Slf4j
@Component
public class JwtUtils {

    public static final String CLAIM_TENANT_ID = "tenantId";
    public static final String CLAIM_USERNAME = "username";
    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${jwt.secret:hawkeye-cloud-secret-key-2026-must-be-at-least-256-bits}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private Long expiration;

    private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
    private SecretKey secretKey;

    public JwtUtils(ReactiveStringRedisTemplate reactiveStringRedisTemplate) {
        this.reactiveStringRedisTemplate = reactiveStringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException("JWT密钥长度不足，必须≥32个字符（256位）");
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

    public boolean validateToken(String token) {
        if (isTokenInBlacklist(token)) {
            log.warn("JWT Token已被加入黑名单");
            return false;
        }
        return parseToken(token) != null;
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
        log.info("Token已加入黑名单，剩余有效时间：{}ms", remainingTime);
        return true;
    }

    public boolean isTokenInBlacklist(String token) {
        if (token == null || token.isBlank()) return false;
        String key = JWT_BLACKLIST_PREFIX + token;
        Boolean exists = reactiveStringRedisTemplate.hasKey(key).block();
        return Boolean.TRUE.equals(exists);
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

    public String generateToken(Long accountId, String username, Long tenantId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_TENANT_ID, tenantId)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }
}
