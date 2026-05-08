package com.hawkeye.vul.common.pojo.vo.vul;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 模板详情 VO
 */
public class VulTemplateVO {

    @Data
    public static class Response {
        private Long templateId;
        private String yamlId;
        private String name;
        private String author;
        private String description;
        private String impact;
        private String severity;
        private Map<String, Object> metadata;
        private String cveId;
        private String cweId;
        private String cvssMetrics;
        private Double cvssScore;
        private Double epssScore;
        private String cpe;
        private String remediation;
        private String flow;
        private Map<String, Object> variables;
        private Boolean enabled;
        private List<String> tags;
        private List<ReferenceVO> references;
        private List<CategoryBriefVO> categories;
        private List<HttpStepVO> httpSteps;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

        @Data
        public static class ReferenceVO {
            private String url;
            private String title;
        }

        @Data
        public static class CategoryBriefVO {
            private Long categoryId;
            private String name;
        }

        @Data
        public static class HttpStepVO {
            private Long httpId;
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
            private List<MatcherVO> matchers;
            private List<ExtractorVO> extractors;
        }

        @Data
        public static class MatcherVO {
            private Long matcherId;
            private String type;
            private String part;
            private String innerCondition;
            private Boolean negative;
            private Boolean caseInsensitive;
            private Boolean internal;
            private Boolean matchAll;
            private String matcherName;
            private Map<String, Object> config;
        }

        @Data
        public static class ExtractorVO {
            private Long extractorId;
            private String type;
            private String part;
            private String extractorName;
            private Map<String, Object> config;
            private Boolean internal;
            private Integer groupNum;
        }
    }
}
