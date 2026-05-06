package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * 匹配器编排器。
 * <p>
 * 负责组合多个匹配器的判定结果，支持 and/or/negative 逻辑。
 * <p>
 * 职责：
 * <ul>
 *   <li>按 outerCondition（步骤级 matchers-condition）组合多个 matcher</li>
 *   <li>处理 negative（结果取反）</li>
 *   <li>不解析 config，只负责编排</li>
 * </ul>
 * <p>
 * 执行流程：
 * <pre>
 *   MatcherPipeline.evaluate(ctx, defs, "or")
 *     ├─ 遍历 defs
 *     ├─ 对每个 def，调用 MatcherRegistry.get(type).match(ctx, def)
 *     ├─ 处理 negative：result = def.isNegative() != result
 *     └─ 按 outerCondition 组合：and（全匹配）/ or（任一匹配）
 * </pre>
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

        // 构建评估器：对每个 def 执行匹配，并处理 negative
        Predicate<MatcherDef> evaluator = def -> {
            var m = registry.get(def.getType());
            boolean result = m.match(ctx, def);
            // negative=true 时取反
            return def.isNegative() != result;
        };

        // 按 outerCondition 组合
        if ("and".equals(outerCondition)) {
            return defs.stream().allMatch(evaluator);
        }
        // 默认 or
        return defs.stream().anyMatch(evaluator);
    }
}
