package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 正则匹配器。
 * <p>
 * 使用 ConcurrentHashMap 缓存编译后的 Pattern，避免重复编译。
 */
@Component
public class RegexMatcher extends AbstractMatcher {

    private final ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<>();

    @Override
    public String type() {
        return "regex";
    }

    @Override
    public boolean match(HttpResponseContext ctx, MatcherDef def) {
        String target = part(ctx, def.getPart());
        if (target == null) return false;

        return evaluateInner(def.getRegex(), def.getCondition(), rule -> {
            try {
                Pattern pattern = patternCache.computeIfAbsent(
                        rule.toString(),
                        r -> Pattern.compile(r, Pattern.DOTALL)
                );
                return pattern.matcher(target).find();
            } catch (Exception e) {
                return false;
            }
        });
    }
}
