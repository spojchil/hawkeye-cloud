package com.hawkeye.auth.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
public class AuthServiceImpl extends ServiceImpl<AuthMapper, Account> implements AuthService {
    private final AuthMapper authMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthLoginVO.ResponseVO login(AuthLoginVO.RequestVO requestVO) {
        Account account = lambdaQuery().eq(Account::getUsername, requestVO.getAccount()).one();
        if (account == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        log.info("账户:{}", account);
        log.info("输入：{}", requestVO);
        if (!passwordEncoder.matches(requestVO.getPassword(), account.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        Long accountId = account.getAccountId();
        Long tenantId = account.getTenantId();
        String token = jwtUtils.generateToken(accountId, tenantId);

        return new AuthLoginVO.ResponseVO(token, accountId, account.getTenantId());
    }
}
