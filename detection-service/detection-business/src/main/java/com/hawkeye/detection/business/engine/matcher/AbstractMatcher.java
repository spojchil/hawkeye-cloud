package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;

import java.util.List;
import java.util.function.Predicate;

/**
 * 匹配器抽象基类——提供 evaluateInner（and/or 组合）和 part（响应提取）两个模板方法
 */
public abstract class AbstractMatcher implements Matcher {

    /**
     * 按 innerCondition 组合多条规则。
     * <p>
     * innerCondition 定义了单个 matcher 内部多条规则之间的关系：
     * - "and"：所有规则都必须匹配
     * - "or"（默认）：任一规则匹配即可
     *
     * @param rules     规则列表
     * @param condition 组合条件（and/or）
     * @param tester    规则测试器
     * @return true 如果组合匹配成功
     */
    protected boolean evaluateInner(List<?> rules, String condition, Predicate<Object> tester) {
        if (rules == null || rules.isEmpty()) return false;

        if ("and".equals(condition)) {
            return rules.stream().allMatch(tester);
        }
        /* 默认 or */
        return rules.stream().anyMatch(tester);
    }

    /**
     * 从 HTTP 响应中提取匹配目标。
     * <p>
     * part 参数指定匹配的响应部分：
     * - "body"（默认）：响应体
     * - "header"：响应头
     * - "all"：响应体 + 响应头
     *
     * @param ctx  HTTP 响应上下文
     * @param part 匹配目标部分
     * @return 匹配目标字符串
     */
    protected String part(HttpResponseContext ctx, String part) {
        if (part == null || "body".equals(part)) {
            return ctx.getBody();
        }
        if ("header".equals(part)) {
            return ctx.getHeaders() != null ? ctx.getHeaders().toString() : "";
        }
        if ("all".equals(part)) {
            return (ctx.getBody() != null ? ctx.getBody() : "")
                    + (ctx.getHeaders() != null ? ctx.getHeaders().toString() : "");
        }
        return ctx.getBody();
    }
}
