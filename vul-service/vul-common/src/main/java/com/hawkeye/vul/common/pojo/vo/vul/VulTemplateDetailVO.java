package com.hawkeye.vul.common.pojo.vo.vul;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 模板详情（含全部关联数据）。 */
@Data
public class VulTemplateDetailVO {
    private Long id;
    private String templateId;
    private String name;
    private String description;
    private String author;
    private String severity;
    private String cveId;
    private String cweId;
    private BigDecimal cvssScore;
    private BigDecimal epssScore;
    private String flow;
    private Map<String, Object> variables;
    private Boolean enabled;
    private Integer version;
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
        private Integer stepOrder;
        private String method;
        private List<String> path;
        private Map<String, String> headers;
        private String body;
        private String raw;
        private String attack;
        private String matchersCondition;
        private Boolean stopAtFirstMatch;
        private List<MatcherVO> matchers;
        private List<ExtractorVO> extractors;
    }

    @Data
    public static class MatcherVO {
        private String type;
        private String part;
        private String condition;
        private Boolean negative;
        private Boolean caseInsensitive;
        private Boolean internal;
        private String name;
        private Map<String, Object> config;
    }

    @Data
    public static class ExtractorVO {
        private String type;
        private String part;
        private String name;
        private Map<String, Object> config;
        private Boolean internal;
        private Integer groupNum;
    }
}
