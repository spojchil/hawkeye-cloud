package com.hawkeye.auth.common.util;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("JwtUtils 工具类单元测试")
class JwtUtilsTest {

    private static final String TEST_SECRET = "test-jwt-secret-key-at-least-256-bits-long!!";
    private static final Long TEST_EXPIRATION = 3_600_000L;

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils(redisTemplate);
        ReflectionTestUtils.setField(jwtUtils, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtils, "expiration", TEST_EXPIRATION);
        jwtUtils.init();

        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
    }

    @Test
    @DisplayName("生成 Token 后能成功解析")
    void generateAndParseToken() {
        String token = jwtUtils.generateToken(1L, "testuser", 100L);
        assertNotNull(token);

        Claims claims = jwtUtils.parseToken(token);
        assertNotNull(claims);
        assertEquals("1", claims.getSubject());
        assertEquals("testuser", claims.get(JwtUtils.CLAIM_USERNAME, String.class));
        assertEquals(100L, claims.get(JwtUtils.CLAIM_TENANT_ID, Long.class));
    }

    @Test
    @DisplayName("解析 null Token 返回 null")
    void parseNullToken() {
        assertNull(jwtUtils.parseToken(null));
    }

    @Test
    @DisplayName("解析空字符串 Token 返回 null")
    void parseBlankToken() {
        assertNull(jwtUtils.parseToken("   "));
    }

    @Test
    @DisplayName("解析格式错误的 Token 返回 null")
    void parseMalformedToken() {
        assertNull(jwtUtils.parseToken("not.a.jwt.token"));
    }

    @Test
    @DisplayName("合法 Token 校验通过")
    void validateValidToken() {
        String token = jwtUtils.generateToken(1L, "admin", 1L);
        assertTrue(jwtUtils.validateToken(token));
    }

    @Test
    @DisplayName("格式错误的 Token 校验失败")
    void validateMalformedToken() {
        assertFalse(jwtUtils.validateToken("garbage-token"));
    }

    @Test
    @DisplayName("getUsernameFromToken 返回正确的用户名")
    void getUsernameFromToken() {
        String token = jwtUtils.generateToken(7L, "operator", 1L);
        assertEquals("operator", jwtUtils.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("getTenantIdFromToken 返回正确的 tenantId")
    void getTenantIdFromToken() {
        String token = jwtUtils.generateToken(1L, "admin", 88L);
        assertEquals(88L, jwtUtils.getTenantIdFromToken(token));
    }

    @Test
    @DisplayName("过期 Token 解析返回 null")
    void parseExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtUtils, "expiration", 1L);
        jwtUtils.init();

        String token = jwtUtils.generateToken(1L, "admin", 1L);
        Thread.sleep(2);

        assertNull(jwtUtils.parseToken(token));
    }
}
