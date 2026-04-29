package com.hawkeye.detection.business.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提取器链——从 HTTP 响应中提取变量，供多步骤模板的后续步骤使用。
 */
@Slf4j
public class ExtractorChain {

    /**
     * 执行所有 extractors，将结果写入 VariableResolver。
     *
     * @param ctx        HTTP 响应上下文
     * @param extractorsJson extractors JSON 字符串
     * @param resolver   变量解析器（提取结果写回此处）
     */
    @SuppressWarnings("unchecked")
    public void extract(HttpResponseContext ctx, String extractorsJson, VariableResolver resolver) {
        if (extractorsJson == null || extractorsJson.isBlank()) return;

        try {
            List<Map<String, Object>> extractors = JSON.parseObject(extractorsJson,
                    new TypeReference<List<Map<String, Object>>>() {});
            if (extractors == null) return;

            for (Map<String, Object> ext : extractors) {
                String type = (String) ext.get("type");
                String name = (String) ext.get("name");
                String part = (String) ext.getOrDefault("part", "body");
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
        } catch (Exception e) {
            log.warn("extractors JSON 解析失败: {}", e.getMessage());
        }
    }

    private String extractRegex(HttpResponseContext ctx, Map<String, Object> ext, String part) {
        List<String> patterns = (List<String>) ext.get("regex");
        if (patterns == null || patterns.isEmpty()) return null;

        String target = getPart(ctx, part);
        if (target == null) return null;

        int group = ext.get("group") != null ? ((Number) ext.get("group")).intValue() : 0;

        for (String pattern : patterns) {
            Matcher m = Pattern.compile(pattern, Pattern.DOTALL).matcher(target);
            if (m.find()) {
                if (group <= m.groupCount()) {
                    return m.group(group);
                }
                return m.group();
            }
        }
        return null;
    }

    private String extractKval(HttpResponseContext ctx, Map<String, Object> ext) {
        List<String> keys = (List<String>) ext.get("kval");
        if (keys == null || ctx.getHeaders() == null) return null;

        for (String key : keys) {
            if (ctx.getHeaders().containsKey(key)) {
                List<String> values = ctx.getHeaders().get(key);
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
