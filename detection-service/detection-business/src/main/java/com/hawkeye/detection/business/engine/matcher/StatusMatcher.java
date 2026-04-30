package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherConfig;

/**
 * Status 匹配器——检查 HTTP 状态码是否在允许列表中。
 */
public class StatusMatcher implements MatcherStrategy {

    @Override
    public String getType() {
        return "status";
    }

    @Override
    public boolean matchRule(HttpResponseContext ctx, MatcherConfig cfg, Object rule) {
        int expected = ((Number) rule).intValue();
        return ctx.getStatusCode() == expected;
    }
}
