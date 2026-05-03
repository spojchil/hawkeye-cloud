package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import org.springframework.stereotype.Component;

@Component
public class WordMatcher extends AbstractMatcher {

    @Override public String type() { return "word"; }

    @Override
    public boolean match(HttpResponseContext ctx, MatcherDef def) {
        String target = part(ctx, def.getPart());
        if (target == null) return false;

        return evaluateInner(def.getWords(), def.getCondition(), rule -> {
            String word = rule.toString();
            if (def.isCaseInsensitive()) {
                return target.toLowerCase().contains(word.toLowerCase());
            }
            return target.contains(word);
        });
    }

    static String part(HttpResponseContext ctx, String part) {
        if (part == null || "body".equals(part)) return ctx.getBody();
        if ("header".equals(part)) return ctx.getHeaders() != null ? ctx.getHeaders().toString() : "";
        if ("all".equals(part)) return (ctx.getBody() != null ? ctx.getBody() : "")
                + (ctx.getHeaders() != null ? ctx.getHeaders().toString() : "");
        return ctx.getBody();
    }
}
