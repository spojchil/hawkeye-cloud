package com.hawkeye.auth.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.auth.business.mapper.AuthMapper;
import com.hawkeye.auth.business.service.AuthService;
import com.hawkeye.auth.common.pojo.entity.Account;
import com.hawkeye.auth.common.pojo.vo.authcontroller.AuthLoginVO;
import com.hawkeye.auth.common.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl extends ServiceImpl<AuthMapper, Account> implements AuthService {

    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthLoginVO.ResponseVO login(AuthLoginVO.RequestVO requestVO) {
        Account account = baseMapper.selectOne(
                new LambdaQueryWrapper<Account>()
                        .eq(Account::getDeletedAt, 0L)
                        .eq(Account::getUsername, requestVO.getUsername()));
        if (account == null) {
            throw new ApiException(CommonErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误",
                    HttpStatus.valueOf(CommonErrorCode.UNAUTHORIZED.getHttpCode()));
        }

        if (!passwordEncoder.matches(requestVO.getPassword(), account.getPassword())) {
            throw new ApiException(CommonErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误",
                    HttpStatus.valueOf(CommonErrorCode.UNAUTHORIZED.getHttpCode()));
        }

        Long accountId = account.getAccountId();
        Long tenantId = account.getTenantId();
        String username = account.getUsername();
        String token = jwtUtils.generateToken(accountId, username, tenantId);

        return new AuthLoginVO.ResponseVO(token, accountId, username, tenantId);
    }
}
