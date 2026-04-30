package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.common.pojo.dto.TaskItemMessage.Extractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提取器链 — 从 HTTP 响应中提取变量，供多步骤模板的后续步骤使用。
 */
@Slf4j
@Component
public class ExtractorChain {

    @SuppressWarnings("unchecked")
    public void extract(HttpResponseContext ctx, List<Extractor> extractors, VariableResolver resolver) {
        if (extractors == null || extractors.isEmpty()) return;

        for (Extractor ext : extractors) {
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
    private String extractRegex(HttpResponseContext ctx, Extractor ext, String part) {
        Map<String, Object> config = ext.getConfig();
        if (config == null) return null;
        Object regexObj = config.get("regex");
        if (!(regexObj instanceof List<?> patterns) || patterns.isEmpty()) return null;

        String target = "header".equals(part)
                ? (ctx.getHeaders() != null ? ctx.getHeaders().toString() : "")
                : (ctx.getBody() != null ? ctx.getBody() : "");
        if (target == null) return null;

        int group = ext.getGroupNum() != null ? ext.getGroupNum() : 0;
        for (Object pattern : patterns) {
            Matcher m = Pattern.compile(pattern.toString(), Pattern.DOTALL).matcher(target);
            if (m.find()) {
                return group <= m.groupCount() ? m.group(group) : m.group();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractKval(HttpResponseContext ctx, Extractor ext) {
        Map<String, Object> config = ext.getConfig();
        if (config == null || ctx.getHeaders() == null) return null;
        Object kvalObj = config.get("kval");
        if (!(kvalObj instanceof List<?> keys)) return null;

        for (Object key : keys) {
            if (ctx.getHeaders().containsKey(key.toString())) {
                List<String> values = ctx.getHeaders().get(key.toString());
                if (values != null && !values.isEmpty()) return values.get(0);
            }
        }
        return null;
    }
}
