package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 提取器编排器。
 * <p>
 * 负责执行多个提取器，将提取的变量写入 VariableContext。
 * <p>
 * 执行流程：
 * <pre>
 *   ExtractorPipeline.extract(ctx, defs, vars)
 *     ├─ 遍历 defs
 *     ├─ 对每个 def，调用 ExtractorRegistry.get(type).extract(ctx, def)
 *     └─ 如果提取到值，写入 vars.set(name, value)
 * </pre>
 * <p>
 * 变量传递：
 * <ul>
 *   <li>提取的变量会写入 VariableContext</li>
 *   <li>后续步骤可以通过 {{变量名}} 引用</li>
 *   <li>例如：提取 token 后，下一步可以在 Header 中使用 {{token}}</li>
 * </ul>
 */
@Component
public class ExtractorPipeline {

    private final ExtractorRegistry registry;

    public ExtractorPipeline(ExtractorRegistry registry) {
        this.registry = registry;
    }

    /**
     * 对一组提取器执行提取，结果写入 VariableContext。
     *
     * @param ctx  HTTP 响应上下文
     * @param defs 提取器定义列表
     * @param vars 变量上下文（提取的值写入此对象）
     */
    public void extract(HttpResponseContext ctx, List<ExtractorDef> defs, VariableContext vars) {
        if (defs == null) return;

        for (ExtractorDef def : defs) {
            // 跳过没有 name 的提取器（无法存储结果）
            if (def.getName() == null) continue;

            // 获取提取器并执行
            var e = registry.get(def.getType());
            String value = e.extract(ctx, def);

            // 提取成功则写入变量上下文
            if (value != null) {
                vars.set(def.getName(), value);
            }
        }
    }
}
