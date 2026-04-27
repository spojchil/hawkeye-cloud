package com.hawkeye.auth.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hawkeye.auth.common.pojo.entity.Account;
import com.hawkeye.auth.common.pojo.vo.authcontroller.AuthLoginVO;

public interface AuthService extends IService<Account> {

    AuthLoginVO.ResponseVO login(AuthLoginVO.RequestVO requestVO);
}
