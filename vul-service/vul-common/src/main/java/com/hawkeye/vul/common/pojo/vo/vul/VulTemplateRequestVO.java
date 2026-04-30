package com.hawkeye.vul.common.pojo.vo.vul;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** 模板创建/更新请求。 */
@Data
public class VulTemplateRequestVO {

    @NotBlank
    private String templateId;

    @NotBlank
    private String name;

    private String description;

    private String author;

    @NotBlank
    @Pattern(regexp = "^(critical|high|medium|low|info|unknown)$",
             message = "severity 必须是 critical/high/medium/low/info/unknown")
    private String severity;

    private String cveId;

    private String cweId;

    @DecimalMax("10.0") @DecimalMin("0.0")
    private BigDecimal cvssScore;

    @DecimalMax("1.0") @DecimalMin("0.0")
    private BigDecimal epssScore;

    private String flow;

    private Map<String, Object> variables;

    private List<String> tags;

    @Valid
    private List<ReferenceRequest> references;

    private List<Long> categoryIds;

    @NotEmpty
    @Valid
    private List<HttpStepRequest> httpSteps;

    @Data
    public static class ReferenceRequest {
        @NotBlank
        private String url;
        private String title;
    }

    @Data
    public static class HttpStepRequest {
        private String method;
        private List<String> path;
        private Map<String, String> headers;
        private String body;
        private String raw;
        private String attack;
        private String matchersCondition;
        private Boolean stopAtFirstMatch;

        @Valid
        private List<MatcherRequest> matchers;

        @Valid
        private List<ExtractorRequest> extractors;
    }

    @Data
    public static class MatcherRequest {
        @NotBlank
        private String type;
        private String part;
        @Pattern(regexp = "^(and|or)$")
        private String condition = "or";
        private Boolean negative;
        private Boolean caseInsensitive;
        private Boolean internal;
        private String name;
        private Map<String, Object> config;
    }

    @Data
    public static class ExtractorRequest {
        @NotBlank
        private String type;
        private String part;
        private String name;
        private Map<String, Object> config;
        private Boolean internal;
        private Integer groupNum = 1;
    }
}
