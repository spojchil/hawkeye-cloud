package com.hawkeye.vul.common.pojo.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 给 detection-service 的检测配置 DTO。
 * 只含执行检测必需的字段，不含描述/作者/CVE等元数据。
 */
@Data
public class VulTemplateDetectDTO {

    private String templateId;
    private String flow;
    private Map<String, Object> variables;
    private List<HttpStepDetect> httpSteps;

    @Data
    public static class HttpStepDetect {
        private Integer stepOrder;
        private String method;
        private List<String> path;
        private Map<String, String> headers;
        private String body;
        private String raw;
        private String attack;
        private String matchersCondition;
        private List<MatcherDetect> matchers;
        private List<ExtractorDetect> extractors;
    }

    @Data
    public static class MatcherDetect {
        private String type;
        private String part;
        private String condition;
        private Boolean negative;
        private Boolean caseInsensitive;
        private Map<String, Object> config;
    }

    @Data
    public static class ExtractorDetect {
        private String type;
        private String part;
        private String name;
        private Map<String, Object> config;
        private Boolean internal;
        private Integer groupNum;
    }
}
