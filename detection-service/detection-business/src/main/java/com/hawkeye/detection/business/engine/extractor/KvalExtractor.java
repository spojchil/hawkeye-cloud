package com.hawkeye.detection.business.engine.extractor;

import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KvalExtractor implements Extractor {

    @Override public String type() { return "kval"; }

    @Override
    public String extract(HttpResponseContext ctx, ExtractorDef def) {
        List<String> keys = def.getKval();
        if (keys == null || keys.isEmpty() || ctx.getHeaders() == null) return null;

        for (String key : keys) {
            if (ctx.getHeaders().containsKey(key)) {
                List<String> values = ctx.getHeaders().get(key);
                if (values != null && !values.isEmpty()) return values.get(0);
            }
        }
        return null;
    }
}
