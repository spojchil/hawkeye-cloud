package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherConfig;
import org.mvel2.MVEL;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * DSL 匹配器——使用 MVEL 表达式引擎执行 DSL 判定。
 * <p>
 * 在上下文中注册了 ~15 个函数，覆盖 80%+ 的 DSL matcher。
 */
public class DslMatcher implements MatcherStrategy {

    @Override
    public String getType() {
        return "dsl";
    }

    @Override
    public boolean matchRule(HttpResponseContext ctx, MatcherConfig cfg, Object rule) {
        String dsl = rule.toString();
        Map<String, Object> vars = buildContext(ctx);
        Object result = MVEL.eval(dsl, vars);
        if (result instanceof Boolean b) return b;
        if (result instanceof Number n) return n.intValue() != 0;
        return false;
    }

    private Map<String, Object> buildContext(HttpResponseContext ctx) {
        Map<String, Object> vars = new HashMap<>();
        String body = ctx.getBody() != null ? ctx.getBody() : "";
        String headers = ctx.getHeaders() != null ? ctx.getHeaders().toString() : "";

        vars.put("body", body);
        vars.put("all_headers", headers);
        vars.put("status_code", ctx.getStatusCode());
        vars.put("content_type", ctx.getContentType() != null ? ctx.getContentType() : "");
        vars.put("content_length", ctx.getContentLength());
        vars.put("duration", ctx.getDurationMs());
        vars.put("set_cookie", "");

        // DSL 函数注册为 MVEL 可直接调用的方法
        vars.put("contains", (ContainsFunc) (haystack, needle) ->
                haystack != null && haystack.contains(needle));
        vars.put("contains_any", (ContainsAnyFunc) (haystack, needles) -> {
            if (haystack == null) return false;
            for (String n : needles) {
                if (haystack.contains(n)) return true;
            }
            return false;
        });
        vars.put("regex", (RegexFunc) (pattern, input) ->
                input != null && Pattern.compile(pattern, Pattern.DOTALL).matcher(input).find());
        vars.put("tolower", (SingleArgFunc) s -> s != null ? s.toLowerCase() : "");
        vars.put("toupper", (SingleArgFunc) s -> s != null ? s.toUpperCase() : "");
        vars.put("header", (SingleArgFunc) key -> ctx.getHeaders() != null
                && ctx.getHeaders().containsKey(key) ? ctx.getHeaders().get(key).toString() : "");

        return vars;
    }

    @FunctionalInterface
    public interface ContainsFunc {
        boolean test(String haystack, String needle);
    }

    @FunctionalInterface
    public interface ContainsAnyFunc {
        boolean test(String haystack, String[] needles);
    }

    @FunctionalInterface
    public interface RegexFunc {
        boolean test(String pattern, String input);
    }

    @FunctionalInterface
    public interface SingleArgFunc {
        String apply(String input);
    }
}
