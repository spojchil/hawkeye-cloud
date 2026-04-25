package com.hawkeye.auth.business.service.impl;

import com.hawkeye.auth.business.mapper.AuthMapper;
import com.hawkeye.auth.business.service.AuthService;
import com.hawkeye.auth.common.pojo.entity.Account;
import com.hawkeye.auth.common.pojo.vo.authcontroller.AuthLoginVO;
import com.hawkeye.auth.common.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final AuthMapper authMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;


    @Override
    public AuthLoginVO.ResponseVO login(AuthLoginVO.RequestVO requestVO) {
        // 1. 根据用户名（account 字段）查询用户
        Account account = authMapper.selectByUsername(requestVO.getAccount());
        if (account == null) {
            // TODO 应该使用常量类，而不是硬编码
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 密码校验
        log.info("账户:{}",account);
        log.info("输入：{}",requestVO);
        if (!passwordEncoder.matches(requestVO.getPassword(), account.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 3. 生成 JWT（需 accountId 和 tenantId）
        Long accountId = account.getAccountId();
        Long tenantId = account.getTenantId();   // BaseEntity 中的 tenantId
        String token = jwtUtils.generateToken(accountId, tenantId);

        return new AuthLoginVO.ResponseVO(token, accountId, account.getTenantId());
    }
}
