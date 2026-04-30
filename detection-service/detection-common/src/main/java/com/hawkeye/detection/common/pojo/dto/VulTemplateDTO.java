package com.hawkeye.detection.common.pojo.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 漏洞模板检测数据 DTO（从 vul-service Feign 获取）。
 * v2: 嵌套 httpSteps 结构，matchers/extractors 内嵌在每个步骤中。
 */
@Data
public class VulTemplateDTO {
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
