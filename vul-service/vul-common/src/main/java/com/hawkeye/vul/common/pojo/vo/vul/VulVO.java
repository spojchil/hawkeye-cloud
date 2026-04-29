package com.hawkeye.vul.common.pojo.vo.vul;

import lombok.Data;

import java.time.LocalDateTime;

public class VulVO {

    @Data
    public static class Request {
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
    }

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
