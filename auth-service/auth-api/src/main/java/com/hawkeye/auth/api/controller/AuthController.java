package com.hawkeye.auth.api.controller;

import com.common.utils.response.ApiResponse;
import com.hawkeye.auth.business.service.AuthService;
import com.hawkeye.auth.common.pojo.vo.authcontroller.AuthLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "登录接口")
public class AuthController {
    private final AuthService authService;


    // TODO 目前只支持了账号密码登录

    /**
     * 登录
     */
    @PostMapping("/login")
    @Operation(summary = "登录")
    public ApiResponse<AuthLoginVO.ResponseVO> login(@RequestBody @Valid AuthLoginVO.RequestVO requestVO) {
        return ApiResponse.success(authService.login(requestVO));
    }
}
