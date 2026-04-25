package com.hawkeye.auth.business.service;

import com.hawkeye.auth.common.pojo.vo.authcontroller.AuthLoginVO;

/**
 * 登录服务接口
 */
public interface AuthService {
    /**
     * 用户登录
     */
    AuthLoginVO.ResponseVO login(AuthLoginVO.RequestVO requestVO);
}
