package com.hawkeye.vul.common.pojo.vo.vul;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class VulTemplatePageVO {

    @Data
    public static class Request {
        private Integer page;
        private Integer pageSize;
        private String name;
        private String severity;
        private String tag;
        private Long categoryId;
        private Boolean enabled;
    }

    @Data
    public static class Response {
        private Long templateId;
        private String yamlId;
        private String name;
        private String severity;
        private String cveId;
        private Double cvssScore;
        private Boolean enabled;
        private List<String> tags;
        private List<String> categories;
        private LocalDateTime createTime;
    }
}
