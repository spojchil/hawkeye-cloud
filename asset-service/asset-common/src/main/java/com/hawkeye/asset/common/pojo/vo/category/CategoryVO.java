package com.hawkeye.asset.common.pojo.vo.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 资产分类 VO
 */
public class CategoryVO {

    @Data
    public static class Request {
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 128, message = "分类名称最长128个字符")
        private String name;

        private Long parentId;

        @Size(max = 500, message = "分类描述最长500个字符")
        private String description;
    }

    @Data
    public static class Response {
        private Long categoryId;
        private String name;
        private Long parentId;
        private String description;
    }
}
