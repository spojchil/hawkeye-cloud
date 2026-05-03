package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import org.springframework.stereotype.Component;

@Component
public class StatusMatcher extends AbstractMatcher {

    @Override public String type() { return "status"; }

    @Override
    public boolean match(HttpResponseContext ctx, MatcherDef def) {
        return evaluateInner(def.getStatus(), def.getCondition(), rule ->
                ctx.getStatusCode() == ((Number) rule).intValue());
    }
}
