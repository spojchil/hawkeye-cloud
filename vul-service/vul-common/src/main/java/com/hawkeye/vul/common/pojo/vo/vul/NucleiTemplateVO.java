package com.hawkeye.vul.common.pojo.vo.vul;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Nuclei 模板请求体 — Spring MVC 自动反序列化 + Bean Validation。
 * http 步骤因结构灵活保留为 Map，其余核心字段全部类型化。
 */
@Data
public class NucleiTemplateVO {

    @NotBlank(message = "模板 id 不能为空")
    @Size(max = 255, message = "id 最长 255 个字符")
    private String id;

    @NotNull(message = "info 不能为空")
    @Valid
    private InfoVO info;

    @Size(max = 1500, message = "flow 最长 1500 个字符")
    private String flow;

    private Map<String, Object> variables;

    /** HTTP 步骤 — 每步结构灵活（path/raw/matchers/extractors 可选），保留 Map */
    private List<Map<String, Object>> http;

    /* info */

    @Data
    public static class InfoVO {

        @NotBlank(message = "模板名称不能为空")
        @Size(max = 255, message = "名称最长 255 个字符")
        private String name;

        @Size(max = 128, message = "作者最长 128 个字符")
        private String author;

        @Size(max = 3000, message = "描述最长 3000 个字符")
        private String description;

        @Size(max = 512, message = "影响说明最长 512 个字符")
        private String impact;

        @NotBlank(message = "严重程度不能为空")
        @Pattern(regexp = "^(critical|high|medium|low|info|unknown)$",
                 message = "severity 必须是 critical/high/medium/low/info/unknown")
        private String severity;

        /** tags 支持逗号分隔字符串 或 字符串数组 */
        private Object tags;

        /** reference 支持 URL 字符串数组 或 对象数组 [{url, title}] */
        private Object reference;

        @Valid
        private ClassificationVO classification;

        private Map<String, Object> metadata;

        @Size(max = 1024, message = "修复建议最长 1024 个字符")
        private String remediation;
    }

    /* classification */

    @Data
    public static class ClassificationVO {

        @JsonProperty("cve-id")
        @Size(max = 50, message = "CVE ID 最长 50 个字符")
        private String cveId;

        @JsonProperty("cwe-id")
        @Size(max = 50, message = "CWE ID 最长 50 个字符")
        private String cweId;

        @JsonProperty("cvss-metrics")
        @Size(max = 128, message = "CVSS 向量最长 128 个字符")
        private String cvssMetrics;

        @JsonProperty("cvss-score")
        private Double cvssScore;

        @JsonProperty("epss-score")
        private Double epssScore;

        @Size(max = 255, message = "CPE 最长 255 个字符")
        private String cpe;
    }
}
