package com.hawkeye.task.common.pojo.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * task-service 本地 Feign 响应 DTO（不依赖 vul-common）。
 * 从 GET /vul/{id} 的 JSON 反序列化，只包含检测调度需要的字段。
 */
@Data
public class VulTemplateBrief {
    private Long templateId;
    private String yamlId;
    private String flow;
    private Map<String, Object> variables;
    private List<HttpStep> httpSteps;

    @Data
    public static class HttpStep {
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
        private List<Matcher> matchers;
        private List<Extractor> extractors;
    }

    @Data
    public static class Matcher {
        private String type;
        private String part;
        private String innerCondition;
        private Boolean negative;
        private Boolean caseInsensitive;
        private Boolean matchAll;
        private Map<String, Object> config;
    }

    @Data
    public static class Extractor {
        private String type;
        private String part;
        private String extractorName;
        private Map<String, Object> config;
        private Boolean internal;
        private Integer groupNum;
    }
}
