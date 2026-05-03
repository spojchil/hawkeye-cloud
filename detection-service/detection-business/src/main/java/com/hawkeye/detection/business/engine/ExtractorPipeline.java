package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExtractorPipeline {
    private final ExtractorRegistry registry;

    public ExtractorPipeline(ExtractorRegistry registry) {
        this.registry = registry;
    }

    /**
     * 对一组提取器执行提取，结果写入 VariableResolver。
     */
    public void extract(HttpResponseContext ctx, List<ExtractorDef> defs, VariableResolver vars) {
        if (defs == null) return;
        for (ExtractorDef def : defs) {
            if (def.getName() == null) continue;
            var e = registry.get(def.getType());
            String value = e.extract(ctx, def);
            if (value != null) {
                vars.put(def.getName(), value);
            }
        }
    }
}
