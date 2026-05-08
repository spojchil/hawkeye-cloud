package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import org.springframework.stereotype.Component;

/**
 * 关键词匹配器——检查响应中是否包含指定关键词，支持大小写敏感和 and/or 组合
 */
@Component
public class WordMatcher extends AbstractMatcher {

    @Override
    public String type() {
        return "word";
    }

    @Override
    public boolean match(HttpResponseContext ctx, MatcherDef def) {
        /* 从响应中提取目标部分（body/header/all） */
        String target = part(ctx, def.getPart());
        if (target == null) return false;

        /* 按 condition 组合多条规则 */
        return evaluateInner(def.getWords(), def.getCondition(), rule -> {
            String word = rule.toString();
            if (def.isCaseInsensitive()) {
                return target.toLowerCase().contains(word.toLowerCase());
            }
            return target.contains(word);
        });
    }
}
