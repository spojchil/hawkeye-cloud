package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherConfig;
import lombok.extern.slf4j.Slf4j;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * DSL 匹配器——使用 MVEL 表达式引擎执行 DSL 判定。
 * <p>
 * DSL 函数通过 {@link ParserContext#addImport} 注册为 MVEL 静态函数。
 * 表达式先编译再执行，编译结果可缓存。
 */
@Slf4j
public class DslMatcher implements MatcherStrategy {

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

    @Override
    public String getType() {
        return "dsl";
    }

    @Override
    public boolean matchRule(HttpResponseContext ctx, MatcherConfig cfg, Object rule) {
        String dsl = preprocess(rule.toString());
        Map<String, Object> vars = buildVars(ctx);
        try {
            Serializable compiled = MVEL.compileExpression(dsl, PARSER_CONTEXT);
            Object result = MVEL.executeExpression(compiled, vars);
            if (result instanceof Boolean b) return b;
            if (result instanceof Number n) return n.intValue() != 0;
            return false;
        } catch (Exception e) {
            throw new RuntimeException("DSL 执行失败: " + dsl + " — " + e.getMessage(), e);
        }
    }

    /**
     * 预处理 DSL 表达式——将 MVEL 冲突的关键词转换为安全形式。
     * MVEL 中 contains / regex 等是内置操作符，不能直接作为函数名。
     * 转换为 Java 方法调用形式。
     */
    private String preprocess(String dsl) {
        // contains(body, 'keyword') → body.contains('keyword')
        dsl = dsl.replaceAll("contains\\(\\s*([a-z_]+)\\s*,\\s*('[^']*')\\s*\\)",
                "$1.contains($2)");
        // regex('pattern', body) → _regex('pattern', body)
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

    /**
     * DSL 辅助函数——通过 {@link ParserContext#addImport} 注册为 MVEL 全局函数。
     * 所有方法必须是 public static，MVEL 通过反射调用。
     */
    public static class DslFunctions {

        public static boolean contains(String haystack, String needle) {
            return haystack != null && haystack.contains(needle);
        }

        public static boolean regex(String pattern, String input) {
            return input != null && Pattern.compile(pattern, Pattern.DOTALL).matcher(input).find();
        }

        public static String tolower(String s) {
            return s != null ? s.toLowerCase() : "";
        }

        public static String toupper(String s) {
            return s != null ? s.toUpperCase() : "";
        }
    }
}
