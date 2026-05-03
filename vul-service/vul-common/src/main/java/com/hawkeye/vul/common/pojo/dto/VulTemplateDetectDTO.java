package com.hawkeye.vul.common.pojo.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class VulTemplateDetectDTO {

    private Long templateId;
    private String yamlId;
    private String flow;
    private Map<String, Object> variables;
    private List<HttpStepDetect> httpSteps;

    @Data
    public static class HttpStepDetect {
        private Integer stepOrder;
        private String httpName;
        private String method;
        private List<String> path;
        private Map<String, String> headers;
        private String body;
        private String raw;
        private String attack;
        private String matchersCondition;
        private Map<String, Object> payloads;
        private Boolean stopAtFirstMatch;
        private Boolean selfContained;
        private Boolean redirects;
        private Integer maxRedirects;
        private Boolean hostRedirects;
        private Boolean unsafe;
        private Boolean cookieReuse;
        private Boolean reqCondition;
        private List<MatcherDetect> matchers;
        private List<ExtractorDetect> extractors;
    }

    @Data
    public static class MatcherDetect {
        private String type;
        private String part;
        private String innerCondition;
        private Boolean negative;
        private Boolean caseInsensitive;
        private Boolean matchAll;
        private Map<String, Object> config;
    }

    @Data
    public static class ExtractorDetect {
        private String type;
        private String part;
        private String extractorName;
        private Map<String, Object> config;
        private Boolean internal;
        private Integer groupNum;
    }
}
