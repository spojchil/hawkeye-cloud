package com.hawkeye.detection.business.engine.matcher;

import java.util.List;
import java.util.function.Predicate;

/**
 * 匹配器基类 — 提供 inner and/or 规则组合逻辑 + part 解析。
 */
public abstract class AbstractMatcher implements Matcher {

    /** 按 innerCondition 组合多条规则 */
    protected boolean evaluateInner(List<?> rules, String condition, Predicate<Object> tester) {
        if (rules == null || rules.isEmpty()) return false;
        if ("and".equals(condition)) {
            return rules.stream().allMatch(tester);
        }
        return rules.stream().anyMatch(tester);
    }
}
