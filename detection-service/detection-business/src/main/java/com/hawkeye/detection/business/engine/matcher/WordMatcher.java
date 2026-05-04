package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import org.springframework.stereotype.Component;

/**
 * 关键词匹配器。
 * <p>
 * 检查 HTTP 响应中是否包含指定的关键词。
 * <p>
 * 匹配逻辑：
 * <ul>
 *   <li>从响应中提取目标部分（body/header/all）</li>
 *   <li>遍历 words 列表，检查是否包含每个关键词</li>
 *   <li>支持大小写敏感/不敏感</li>
 *   <li>支持 and/or 组合条件</li>
 * </ul>
 * <p>
 * 配置示例：
 * <pre>
 * {
 *   "type": "word",
 *   "part": "body",
 *   "condition": "or",
 *   "words": ["admin", "root", "password"],
 *   "caseInsensitive": true
 * }
 * </pre>
 */
@Component
public class WordMatcher extends AbstractMatcher {

    @Override
    public String type() {
        return "word";
    }

    @Override
    public boolean match(HttpResponseContext ctx, MatcherDef def) {
        // 获取匹配目标
        String target = part(ctx, def.getPart());
        if (target == null) return false;

        // 按 condition 组合多条规则
        return evaluateInner(def.getWords(), def.getCondition(), rule -> {
            String word = rule.toString();
            if (def.isCaseInsensitive()) {
                return target.toLowerCase().contains(word.toLowerCase());
            }
            return target.contains(word);
        });
    }
}
