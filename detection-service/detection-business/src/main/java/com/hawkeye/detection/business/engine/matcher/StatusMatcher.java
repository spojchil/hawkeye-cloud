package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import org.springframework.stereotype.Component;

/**
 * HTTP 状态码匹配器。
 * 检查 HTTP 响应状态码是否在指定列表中。
 */
@Component
public class StatusMatcher extends AbstractMatcher {

    @Override
    public String type() {
        return "status";
    }

    @Override
    public boolean match(HttpResponseContext ctx, MatcherDef def) {
        return evaluateInner(def.getStatus(), def.getCondition(), rule ->
                ctx.getStatusCode() == ((Number) rule).intValue());
    }
}
