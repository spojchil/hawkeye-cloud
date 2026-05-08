package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * 匹配器编排器——通过 MatcherRegistry 按 type 获取匹配器，组合 and/or/negative 判定
 */
@Component
public class MatcherPipeline {

    private final MatcherRegistry registry;

    public MatcherPipeline(MatcherRegistry registry) {
        this.registry = registry;
    }

    /**
     * 对一组 matcher 执行组合判定。
     *
     * @param ctx            HTTP 响应
     * @param defs           匹配器定义列表
     * @param outerCondition 外层 and/or（步骤级 matchers-condition）
     * @return true 如果整体匹配
     */
    public boolean evaluate(HttpResponseContext ctx, List<MatcherDef> defs, String outerCondition) {
        if (defs == null || defs.isEmpty()) return false;

        /* 构建评估器：对每个 def 执行匹配，negative=true 时取反 */
        Predicate<MatcherDef> evaluator = def -> {
            var m = registry.get(def.getType());
            boolean result = m.match(ctx, def);
            return def.isNegative() != result;
        };
        /* 按 outerCondition 组合结果，"and" 全匹配，默认 "or" */
        if ("and".equals(outerCondition)) {
            return defs.stream().allMatch(evaluator);
        }
        return defs.stream().anyMatch(evaluator);
    }
}
