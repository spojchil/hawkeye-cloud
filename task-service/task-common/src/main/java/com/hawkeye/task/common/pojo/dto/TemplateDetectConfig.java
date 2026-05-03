package com.hawkeye.task.common.pojo.dto;

import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateVO;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 模板检测配置（从 VulTemplateVO.Response 修剪，只保留执行必需字段）。
 * <p>
 * 去掉了 name/author/description/severity/cveId/tags/categories/references
 * 等检测引擎不需要的元数据，减小消息体和缓存体积。
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
        private Boolean stopAtFirstMatch;
        private Boolean selfContained;
        private Boolean redirects;
        private Integer maxRedirects;
        private Boolean hostRedirects;
        private Boolean unsafe;
        private Boolean cookieReuse;
        private Boolean reqCondition;
        private List<MatcherConfig> matchers;
        private List<ExtractorConfig> extractors;
    }

    @Data
    public static class MatcherConfig {
        private String type;
        private String part;
        private String innerCondition;
        private Boolean negative;
        private Boolean caseInsensitive;
        private Boolean matchAll;
        private Map<String, Object> config;
    }

    @Data
    public static class ExtractorConfig {
        private String type;
        private String part;
        private String extractorName;
        private Map<String, Object> config;
        private Boolean internal;
        private Integer groupNum;
    }

    /**
     * 从 VulTemplateVO.Response 修剪出检测所需字段。
     */
    public static TemplateDetectConfig from(VulTemplateVO.Response vo) {
        TemplateDetectConfig cfg = new TemplateDetectConfig();
        cfg.setYamlId(vo.getYamlId());
        cfg.setFlow(vo.getFlow());
        cfg.setVariables(vo.getVariables());
        if (vo.getHttpSteps() != null) {
            cfg.setHttpSteps(vo.getHttpSteps().stream().map(s -> {
                HttpStepConfig hs = new HttpStepConfig();
                hs.setStepOrder(s.getStepOrder());
                hs.setHttpName(s.getHttpName());
                hs.setMethod(s.getMethod());
                hs.setPath(s.getPath());
                hs.setHeaders(s.getHeaders());
                hs.setBody(s.getBody());
                hs.setRaw(s.getRaw());
                hs.setAttack(s.getAttack());
                hs.setMatchersCondition(s.getMatchersCondition());
                hs.setPayloads(s.getPayloads());
                hs.setStopAtFirstMatch(s.getStopAtFirstMatch());
                hs.setSelfContained(s.getSelfContained());
                hs.setRedirects(s.getRedirects());
                hs.setMaxRedirects(s.getMaxRedirects());
                hs.setHostRedirects(s.getHostRedirects());
                hs.setUnsafe(s.getUnsafe());
                hs.setCookieReuse(s.getCookieReuse());
                hs.setReqCondition(s.getReqCondition());
                if (s.getMatchers() != null) {
                    hs.setMatchers(s.getMatchers().stream().map(m -> {
                        MatcherConfig mc = new MatcherConfig();
                        mc.setType(m.getType()); mc.setPart(m.getPart());
                        mc.setInnerCondition(m.getInnerCondition());
                        mc.setNegative(m.getNegative()); mc.setCaseInsensitive(m.getCaseInsensitive());
                        mc.setMatchAll(m.getMatchAll()); mc.setConfig(m.getConfig());
                        return mc;
                    }).toList());
                }
                if (s.getExtractors() != null) {
                    hs.setExtractors(s.getExtractors().stream().map(e -> {
                        ExtractorConfig ec = new ExtractorConfig();
                        ec.setType(e.getType()); ec.setPart(e.getPart());
                        ec.setExtractorName(e.getExtractorName()); ec.setConfig(e.getConfig());
                        ec.setInternal(e.getInternal()); ec.setGroupNum(e.getGroupNum());
                        return ec;
                    }).toList());
                }
                return hs;
            }).toList());
        }
        return cfg;
    }
}
