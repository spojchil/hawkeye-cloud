package com.hawkeye.auth.common.pojo.vo.authcontroller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

public class AuthLoginVO {

    @Data
    public static class RequestVO {
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 30, message = "用户名长度应在3-30个字符之间")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
        private String username;

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度应在6-32个字符之间")
        private String password;
    }

    @Data
    @AllArgsConstructor
    public static class ResponseVO {
        private String token;
        private Long accountId;
        private String username;
        private Long tenantId;
    }
}
