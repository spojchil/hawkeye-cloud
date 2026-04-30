package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.common.pojo.dto.VulTemplateDTO.ExtractorDetect;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提取器链（v2 适配）。
 * 从 HTTP 响应中提取变量，供多步骤模板的后续步骤使用。
 */
@Slf4j
@Component
public class ExtractorChain {

    /**
     * v2 接口：直接接受 ExtractorDetect 列表。
     */
    @SuppressWarnings("unchecked")
    public void extract(HttpResponseContext ctx, List<ExtractorDetect> extractors, VariableResolver resolver) {
        if (extractors == null || extractors.isEmpty()) return;

        for (ExtractorDetect ext : extractors) {
            String type = ext.getType();
            String name = ext.getName();
            String part = ext.getPart() != null ? ext.getPart() : "body";
            if (name == null) continue;

            String value = null;
            if ("regex".equals(type)) {
                value = extractRegex(ctx, ext, part);
            } else if ("kval".equals(type)) {
                value = extractKval(ctx, ext);
            }

            if (value != null) {
                resolver.put(name, value);
                log.debug("Extract: {} = {}", name, value);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String extractRegex(HttpResponseContext ctx, ExtractorDetect ext, String part) {
        Map<String, Object> config = ext.getConfig();
        if (config == null) return null;
        Object regexObj = config.get("regex");
        if (!(regexObj instanceof List<?> patterns) || patterns.isEmpty()) return null;

        String target = getPart(ctx, part);
        if (target == null) return null;

        int group = ext.getGroupNum() != null ? ext.getGroupNum() : 0;

        for (Object pattern : patterns) {
            Matcher m = Pattern.compile(pattern.toString(), Pattern.DOTALL).matcher(target);
            if (m.find()) {
                if (group <= m.groupCount()) {
                    return m.group(group);
                }
                return m.group();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractKval(HttpResponseContext ctx, ExtractorDetect ext) {
        Map<String, Object> config = ext.getConfig();
        if (config == null || ctx.getHeaders() == null) return null;
        Object kvalObj = config.get("kval");
        if (!(kvalObj instanceof List<?> keys)) return null;

        for (Object key : keys) {
            if (ctx.getHeaders().containsKey(key.toString())) {
                List<String> values = ctx.getHeaders().get(key.toString());
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                }
            }
        }
        return null;
    }

    private String getPart(HttpResponseContext ctx, String part) {
        if ("header".equals(part)) {
            return ctx.getHeaders() != null ? ctx.getHeaders().toString() : "";
        }
        return ctx.getBody() != null ? ctx.getBody() : "";
    }
}
