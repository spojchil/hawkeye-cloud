package com.hawkeye.detection.business.engine.matcher;

import com.googlecode.aviator.AviatorEvaluator;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * DSL 表达式匹配器。
 * <p>
 * 使用 Aviator 表达式引擎执行复杂的匹配逻辑。
 * <p>
 * 支持的 DSL 函数：
 * <ul>
 *   <li>contains(str, substr) - 字符串包含</li>
 *   <li>contains_all(str, ...) - 全部包含</li>
 *   <li>contains_any(str, ...) - 任一包含</li>
 *   <li>regex(pattern, input) - 正则匹配</li>
 *   <li>tolower(str) - 转小写</li>
 *   <li>toupper(str) - 转大写</li>
 * </ul>
 * <p>
 * 可用变量：
 * <ul>
 *   <li>body - 响应体</li>
 *   <li>status_code - 状态码</li>
 *   <li>content_type - 内容类型</li>
 *   <li>all_headers - 所有响应头</li>
 * </ul>
 */
@Slf4j
@Component
public class DslMatcher extends AbstractMatcher {

    /** 正则缓存 */
    private final Map<String, Pattern> patternCache = new HashMap<>();

    @Override
    public String type() {
        return "dsl";
    }

    @Override
    public boolean match(HttpResponseContext ctx, MatcherDef def) {
        Map<String, Object> vars = buildVars(ctx);

        return evaluateInner(def.getDsl(), def.getCondition(), rule -> {
            String dsl = preprocess(rule.toString());
            try {
                Object result = AviatorEvaluator.execute(dsl, vars, true);
                return toBoolean(result);
            } catch (Exception e) {
                log.warn("DSL 执行失败: {}, 错误: {}", dsl, e.getMessage());
                return false;
            }
        });
    }

    /**
     * 预处理 DSL 表达式。
     * <p>
     * 将 Nuclei 风格的函数调用转换为 Aviator 语法。
     */
    private String preprocess(String dsl) {
        // contains(body, 'text') → string.contains(body, 'text')
        dsl = dsl.replaceAll(
                "contains\\(\\s*([a-z_]+)\\s*,\\s*('[^']*')\\s*\\)",
                "string.contains($1, $2)");
        // contains_all(body, 'a', 'b') → string.contains_all(body, 'a', 'b')
        dsl = dsl.replaceAll(
                "contains_all\\(\\s*([a-z_]+)\\s*,",
                "string.contains_all($1,");
        // regex('pattern', body) → string.regex('pattern', body)
        dsl = dsl.replaceAll(
                "regex\\(\\s*('[^']*')\\s*,\\s*([a-z_]+)\\s*\\)",
                "string.regex($1, $2)");
        return dsl;
    }

    /**
     * 构建 DSL 变量上下文。
     */
    private Map<String, Object> buildVars(HttpResponseContext ctx) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("body", ctx.getBody() != null ? ctx.getBody() : "");
        vars.put("all_headers", ctx.getHeaders() != null ? ctx.getHeaders().toString() : "");
        vars.put("status_code", ctx.getStatusCode());
        vars.put("content_type", ctx.getContentType() != null ? ctx.getContentType() : "");
        vars.put("content_length", ctx.getContentLength());
        vars.put("duration", ctx.getDurationMs());
        return vars;
    }

    /**
     * 将结果转换为布尔值。
     */
    private boolean toBoolean(Object result) {
        if (result instanceof Boolean b) {
            return b;
        } else if (result instanceof Number n) {
            return n.intValue() != 0;
        } else if (result instanceof String s) {
            return !s.isEmpty();
        }
        return result != null;
    }

    /**
     * 静态内部类：提供 DSL 函数实现。
     * <p>
     * 这些函数会被注册到 Aviator 中供 DSL 表达式调用。
     */
    public static class DslFunctions {
        /**
         * 字符串包含。
         */
        public static boolean contains(String str, String substr) {
            if (str == null || substr == null) return false;
            return str.contains(substr);
        }

        /**
         * 全部包含。
         */
        public static boolean contains_all(String str, String... substrs) {
            if (str == null || substrs == null) return false;
            for (String substr : substrs) {
                if (!str.contains(substr)) return false;
            }
            return true;
        }

        /**
         * 正则匹配。
         */
        public static boolean regex(String pattern, String input) {
            if (pattern == null || input == null) return false;
            try {
                return Pattern.compile(pattern, Pattern.DOTALL).matcher(input).find();
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * 转小写。
         */
        public static String tolower(String s) {
            return s != null ? s.toLowerCase() : "";
        }

        /**
         * 转大写。
         */
        public static String toupper(String s) {
            return s != null ? s.toUpperCase() : "";
        }
    }
}
