package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
public class DslMatcher extends AbstractMatcher {

    private static final ParserContext PARSER_CONTEXT = new ParserContext();

    static {
        try {
            PARSER_CONTEXT.addImport("_regex",
                    DslFunctions.class.getMethod("regex", String.class, String.class));
            PARSER_CONTEXT.addImport("tolower",
                    DslFunctions.class.getMethod("tolower", String.class));
            PARSER_CONTEXT.addImport("toupper",
                    DslFunctions.class.getMethod("toupper", String.class));
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override public String type() { return "dsl"; }

    @Override
    public boolean match(HttpResponseContext ctx, MatcherDef def) {
        Map<String, Object> vars = buildVars(ctx);
        return evaluateInner(def.getDsl(), def.getCondition(), rule -> {
            String dsl = preprocess(rule.toString());
            try {
                Serializable compiled = MVEL.compileExpression(dsl, PARSER_CONTEXT);
                Object result = MVEL.executeExpression(compiled, vars);
                return result instanceof Boolean b ? b
                        : result instanceof Number n ? n.intValue() != 0 : false;
            } catch (Exception e) {
                throw new RuntimeException("DSL 执行失败: " + dsl, e);
            }
        });
    }

    private String preprocess(String dsl) {
        dsl = dsl.replaceAll("contains\\(\\s*([a-z_]+)\\s*,\\s*('[^']*')\\s*\\)", "$1.contains($2)");
        dsl = dsl.replace("regex(", "_regex(");
        return dsl;
    }

    private Map<String, Object> buildVars(HttpResponseContext ctx) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("body", ctx.getBody() != null ? ctx.getBody() : "");
        vars.put("all_headers", ctx.getHeaders() != null ? ctx.getHeaders().toString() : "");
        vars.put("status_code", ctx.getStatusCode());
        vars.put("content_type", ctx.getContentType() != null ? ctx.getContentType() : "");
        vars.put("content_length", ctx.getContentLength());
        vars.put("duration", ctx.getDurationMs());
        vars.put("header", (java.util.function.Function<String, String>) key ->
                ctx.getHeaders() != null && ctx.getHeaders().containsKey(key)
                        ? ctx.getHeaders().get(key).toString() : "");
        return vars;
    }

    public static class DslFunctions {
        public static boolean regex(String pattern, String input) {
            return input != null && Pattern.compile(pattern, Pattern.DOTALL).matcher(input).find();
        }
        public static String tolower(String s) { return s != null ? s.toLowerCase() : ""; }
        public static String toupper(String s) { return s != null ? s.toUpperCase() : ""; }
    }
}
