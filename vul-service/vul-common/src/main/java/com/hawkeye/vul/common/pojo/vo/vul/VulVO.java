package com.hawkeye.vul.common.pojo.vo.vul;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 漏洞模板 VO。
 * <p>
 * Request 用于创建/更新请求体，含 Jakarta Validation 约束；
 * Response 用于详情返回值，区别于分页列表用的 {@link PageVulVO.Response}。
 * 列表查询不应暴露完整 JSON 字段（httpRequests/matchers/extractors），
 * 因此分页响应使用独立的精简 VO。
 */
public class VulVO {

    @Data
    public static class Request {
        @NotBlank(message = "模板标识不能为空")
        private String templateId;

        @NotBlank(message = "模板名称不能为空")
        private String name;

        private String description;
        private String author;

        @NotBlank(message = "严重程度不能为空")
        private String severity;

        private String tags;
        private String reference;
        private String classification;
        private String metadata;
        private String flow;
        private String variables;
        private String httpRequests;
        private String matchers;
        private String extractors;
        private Boolean enabled;
        private Integer version;
    }

    /**
     * 漏洞模板详情返回值（仅限详情接口使用）。
     * <p>
     * ★ 包含 httpRequests / matchers / extractors 三个 MEDIUMTEXT 字段，
     *    数据量大（单条可达数百 KB），严禁在分页列表接口中复用此 VO。
     *    分页列表请使用 {@link PageVulVO.Response}。
     */
    @Data
    public static class Response {
        private Long id;
        private String templateId;
        private String name;
        private String description;
        private String author;
        private String severity;
        private String tags;
        private String reference;
        private String classification;
        private String metadata;
        private String flow;
        private String variables;
        private String httpRequests;
        private String matchers;
        private String extractors;
        private Boolean enabled;
        private Integer version;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;
    }
}
