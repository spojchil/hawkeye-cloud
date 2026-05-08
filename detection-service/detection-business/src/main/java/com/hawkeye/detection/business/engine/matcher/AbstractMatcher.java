package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;

import java.util.List;
import java.util.function.Predicate;

/**
 * 匹配器抽象基类——提供 evaluateInner（and/or 组合）和 part（响应提取）两个模板方法
 */
public abstract class AbstractMatcher implements Matcher {

    /**
     * 按 innerCondition 组合多条规则。"and" 全匹配，"or" 任一匹配。
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
     * 从 HTTP 响应中提取匹配目标。part 为空或 "body" 取响应体，"header" 取响应头，"all" 取两者。
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
