package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherConfig;

/**
 * Word 匹配器——检查响应中是否包含指定关键词。
 */
public class WordMatcher implements MatcherStrategy {

    @Override
    public String getType() {
        return "word";
    }

    @Override
    public boolean matchRule(HttpResponseContext ctx, MatcherConfig cfg, Object rule) {
        String word = rule.toString();
        String target = getPart(ctx, cfg.getPart());
        if (target == null) return false;

        if (cfg.isCaseInsensitive()) {
            return target.toLowerCase().contains(word.toLowerCase());
        }
        return target.contains(word);
    }

    private String getPart(HttpResponseContext ctx, String part) {
        if (part == null || "body".equals(part)) return ctx.getBody();
        if ("header".equals(part)) return ctx.getHeaders() != null ? ctx.getHeaders().toString() : "";
        if ("all".equals(part)) return (ctx.getBody() != null ? ctx.getBody() : "")
                + (ctx.getHeaders() != null ? ctx.getHeaders().toString() : "");
        return ctx.getBody();
    }
}
