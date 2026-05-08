package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 提取器编排器——通过 ExtractorRegistry 按 type 获取提取器，执行后将值写入 VariableContext
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
            /* 跳过没有 name 的提取器（无法存储结果） */
            var e = registry.get(def.getType());
            String value = e.extract(ctx, def);
            /* 提取成功则写入变量上下文 */
            if (value != null) {
                vars.set(def.getName(), value);
            }
        }
    }
}
