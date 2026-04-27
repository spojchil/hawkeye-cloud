package com.hawkeye.asset.common.pojo.vo.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类 VO（视图对象）
 * <p>
 * 分类支持树形结构（通过 parentId 指向父分类）。
 */
public class CategoryVO {

    /**
     * 分类创建/更新请求体。
     * parentId 不填或传 null 表示创建顶级分类。
     */
    @Data
    public static class Request {
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 128, message = "分类名称最长128个字符")
        private String name;

        private Long parentId;

        @Size(max = 500, message = "分类描述最长500个字符")
        private String description;
    }

    /**
     * 分类返回值。
     */
    @Data
    public static class Response {
        private Long categoryId;
        private String name;
        private Long parentId;
        private String description;
    }
}
