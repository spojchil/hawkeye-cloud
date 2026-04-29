package com.hawkeye.vul.common.pojo.vo.vul;

import lombok.Data;

public class PageVulVO {

    @Data
    public static class Request {
        private Integer page;
        private Integer pageSize;
        private String name;
        private String severity;
        private String tags;
        private Long categoryId;
        private Boolean enabled;
    }

    @Data
    public static class Response {
        private Long id;
        private String templateId;
        private String name;
        private String severity;
        private String tags;
        private Boolean enabled;
        private Integer version;
    }
}
