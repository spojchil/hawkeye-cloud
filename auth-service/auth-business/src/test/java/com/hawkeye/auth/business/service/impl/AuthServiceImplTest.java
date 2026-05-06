package com.hawkeye.auth.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hawkeye.auth.business.mapper.AuthMapper;
import com.hawkeye.auth.common.pojo.entity.Account;
import com.hawkeye.auth.common.pojo.vo.authcontroller.AuthLoginVO;
import com.hawkeye.auth.common.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl 登录逻辑单元测试")
class AuthServiceImplTest {

    @Mock
    private AuthMapper authMapper;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "baseMapper", authMapper);
    }

    @Test
    @DisplayName("登录成功 — 用户存在且密码正确，返回 token 和账号信息")
    void loginSuccess() {
        AuthLoginVO.RequestVO request = new AuthLoginVO.RequestVO();
        request.setUsername("admin");
        request.setPassword("password");

        Account account = buildAccount(1L, "admin", "$2a$10$hashed", 1L);

        when(authMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(passwordEncoder.matches("password", "$2a$10$hashed")).thenReturn(true);
        when(jwtUtils.generateToken(1L, "admin", 1L)).thenReturn("jwt-token-abc");

        AuthLoginVO.ResponseVO response = authService.login(request);

        assertAll("登录成功验证",
                () -> assertEquals("jwt-token-abc", response.getToken()),
                () -> assertEquals(1L, response.getAccountId()),
                () -> assertEquals(1L, response.getTenantId())
        );

        verify(jwtUtils).generateToken(1L, "admin", 1L);
    }

    @Test
    @DisplayName("登录失败 — 用户不存在，抛出 RuntimeException")
    void loginFailUserNotFound() {
        AuthLoginVO.RequestVO request = new AuthLoginVO.RequestVO();
        request.setUsername("nobody");
        request.setPassword("password");

        when(authMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(request));

        assertEquals("用户名或密码错误", ex.getMessage());
        // 用户不存在时，不应调用密码校验和 JWT 生成
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtUtils, never()).generateToken(anyLong(), anyString(), anyLong());
    }

    @Test
    @DisplayName("登录失败 — 密码错误，抛出 RuntimeException")
    void loginFailWrongPassword() {
        AuthLoginVO.RequestVO request = new AuthLoginVO.RequestVO();
        request.setUsername("admin");
        request.setPassword("wrong_password");

        Account account = buildAccount(1L, "admin", "$2a$10$hashed", 1L);

        when(authMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        when(passwordEncoder.matches("wrong_password", "$2a$10$hashed")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.login(request));

        assertEquals("用户名或密码错误", ex.getMessage());
        // 密码错误时，不应调用 JWT 生成
        verify(jwtUtils, never()).generateToken(anyLong(), anyString(), anyLong());
    }

    private Account buildAccount(Long id, String username, String password, Long tenantId) {
        Account account = new Account();
        account.setAccountId(id);
        account.setUsername(username);
        account.setPassword(password);
        account.setTenantId(tenantId);
        return account;
    }
}
