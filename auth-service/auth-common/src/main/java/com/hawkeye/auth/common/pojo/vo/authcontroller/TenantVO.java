package com.hawkeye.auth.common.pojo.vo.authcontroller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class TenantVO {

    @Data
    public static class Request {
        @NotBlank(message = "租户名称不能为空")
        @Size(min = 2, max = 64, message = "租户名称长度应在2-64个字符之间")
        private String name;

        @Size(max = 128, message = "联系邮箱长度不能超过128个字符")
        private String contactEmail;

        private Integer status;

        private Integer maxAssets;

        private Integer maxUsers;

        private Integer maxTasks;
    }

    @Data
    public static class Response {
        private Long tenantId;
        private String name;
        private String contactEmail;
        private Integer status;
        private Integer maxAssets;
        private Integer maxUsers;
        private Integer maxTasks;
    }
}
