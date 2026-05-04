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

/**
 * DSL 表达式匹配器。
 * <p>
 * 使用 MVEL 表达式引擎执行复杂的匹配逻辑。
 * <p>
 * 支持的 DSL 函数：
 * <ul>
 *   <li>contains(str, substr) - 字符串包含</li>
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
 *   <li>header(key) - 获取指定响应头</li>
 * </ul>
 * <p>
 * 配置示例：
 * <pre>
 * {
 *   "type": "dsl",
 *   "condition": "and",
 *   "dsl": [
 *     "contains(body, 'admin') && status_code == 200",
 *     "!contains(all_headers, 'X-Frame-Options')"
 *   ]
 * }
 * </pre>
 * <p>
 * 安全说明：
 * - MVEL 可执行任意 Java 代码，有 RCE 风险
 * - 建议后续迁移到 Aviator（沙箱安全）
 */
@Slf4j
@Component
public class DslMatcher extends AbstractMatcher {

    /** MVEL 解析器上下文（预注册安全函数） */
    private static final ParserContext PARSER_CONTEXT = new ParserContext();

    static {
        try {
            // 注册安全的函数
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
    public String type() {
        return "dsl";
    }

    @Override
    public boolean match(HttpResponseContext ctx, MatcherDef def) {
        Map<String, Object> vars = buildVars(ctx);

        return evaluateInner(def.getDsl(), def.getCondition(), rule -> {
            String dsl = preprocess(rule.toString());
            try {
                Serializable compiled = MVEL.compileExpression(dsl, PARSER_CONTEXT);
                Object result = MVEL.executeExpression(compiled, vars);

                // 处理返回值
                if (result instanceof Boolean b) {
                    return b;
                } else if (result instanceof Number n) {
                    return n.intValue() != 0;
                }
                return false;
            } catch (Exception e) {
                throw new RuntimeException("DSL 执行失败: " + dsl, e);
            }
        });
    }

    /**
     * 预处理 DSL 表达式。
     * <p>
     * 将 Nuclei 风格的函数调用转换为 MVEL 语法。
     */
    private String preprocess(String dsl) {
        // contains(body, 'text') → body.contains('text')
        dsl = dsl.replaceAll("contains\\(\\s*([a-z_]+)\\s*,\\s*('[^']*')\\s*\\)", "$1.contains($2)");
        // regex( → _regex(（使用注册的安全函数）
        dsl = dsl.replace("regex(", "_regex(");
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
        // header 函数：获取指定响应头
        vars.put("header", (java.util.function.Function<String, String>) key ->
                ctx.getHeaders() != null && ctx.getHeaders().containsKey(key)
                        ? ctx.getHeaders().get(key).toString() : "");
        return vars;
    }

    /**
     * DSL 安全函数。
     */
    public static class DslFunctions {
        /**
         * 正则匹配函数。
         */
        public static boolean regex(String pattern, String input) {
            return input != null && Pattern.compile(pattern, Pattern.DOTALL).matcher(input).find();
        }

        /**
         * 转小写函数。
         */
        public static String tolower(String s) {
            return s != null ? s.toLowerCase() : "";
        }

        /**
         * 转大写函数。
         */
        public static String toupper(String s) {
            return s != null ? s.toUpperCase() : "";
        }
    }
}
