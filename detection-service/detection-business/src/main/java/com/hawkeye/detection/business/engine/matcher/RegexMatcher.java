package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class RegexMatcher extends AbstractMatcher {

    @Override public String type() { return "regex"; }

    @Override
    public boolean match(HttpResponseContext ctx, MatcherDef def) {
        String target = WordMatcher.part(ctx, def.getPart());
        if (target == null) return false;

        return evaluateInner(def.getRegex(), def.getCondition(), rule -> {
            try {
                return Pattern.compile(rule.toString(), Pattern.DOTALL).matcher(target).find();
            } catch (Exception e) {
                return false;
            }
        });
    }
}
