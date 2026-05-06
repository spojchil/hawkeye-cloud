package com.hawkeye.task.common.pojo.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模板检测配置（从 VulTemplateBrief 修剪，只保留执行必需字段，去掉 httpId 等）。
 */
@Data
public class TemplateDetectConfig {

    private String yamlId;
    private String flow;
    private Map<String, Object> variables;
    private List<HttpStepConfig> httpSteps;

    @Data
    public static class HttpStepConfig {
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
        private Boolean stopAtFirstMatch, selfContained, redirects, hostRedirects, unsafe, cookieReuse, reqCondition;
        private Integer maxRedirects;
        private List<MatcherConfig> matchers;
        private List<ExtractorConfig> extractors;
    }

    @Data
    public static class MatcherConfig {
        private String type, part, innerCondition;
        private Boolean negative, caseInsensitive, matchAll;
        private Map<String, Object> config;
    }

    @Data
    public static class ExtractorConfig {
        private String type, part, extractorName;
        private Map<String, Object> config;
        private Boolean internal;
        private Integer groupNum;
    }

    public static TemplateDetectConfig from(VulTemplateBrief b) {
        var c = new TemplateDetectConfig();
        c.yamlId = b.getYamlId();
        c.flow = b.getFlow();
        c.variables = b.getVariables();
        if (b.getHttpSteps() != null) c.httpSteps = b.getHttpSteps().stream().map(s -> {
            var h = new HttpStepConfig();
            h.stepOrder = s.getStepOrder();
            h.httpName = s.getHttpName();
            h.method = s.getMethod();
            h.path = s.getPath();
            h.headers = s.getHeaders();
            h.body = s.getBody();
            h.raw = s.getRaw();
            h.attack = s.getAttack();
            h.matchersCondition = s.getMatchersCondition();
            h.payloads = s.getPayloads();
            h.stopAtFirstMatch = s.getStopAtFirstMatch();
            h.selfContained = s.getSelfContained();
            h.redirects = s.getRedirects();
            h.maxRedirects = s.getMaxRedirects();
            h.hostRedirects = s.getHostRedirects();
            h.unsafe = s.getUnsafe();
            h.cookieReuse = s.getCookieReuse();
            h.reqCondition = s.getReqCondition();
            if (s.getMatchers() != null) h.matchers = s.getMatchers().stream().map(m -> {
                var mc = new MatcherConfig();
                mc.type = m.getType();
                mc.part = m.getPart();
                mc.innerCondition = m.getInnerCondition();
                mc.negative = m.getNegative();
                mc.caseInsensitive = m.getCaseInsensitive();
                mc.matchAll = m.getMatchAll();
                mc.config = m.getConfig();
                return mc;
            }).toList();
            if (s.getExtractors() != null) h.extractors = s.getExtractors().stream().map(e -> {
                var ec = new ExtractorConfig();
                ec.type = e.getType();
                ec.part = e.getPart();
                ec.extractorName = e.getExtractorName();
                ec.config = e.getConfig();
                ec.internal = e.getInternal();
                ec.groupNum = e.getGroupNum();
                return ec;
            }).toList();
            return h;
        }).toList();
        return c;
    }
}
