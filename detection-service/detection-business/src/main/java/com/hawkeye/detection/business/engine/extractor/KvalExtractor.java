package com.hawkeye.detection.business.engine.extractor;

import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 键值提取器——从 HTTP 响应头中提取指定键的值
 *
 * <p>配置示例：{"type":"kval","part":"header","name":"session_id","kval":["Set-Cookie"]}</p>
 */
@Component
public class KvalExtractor implements Extractor {

    @Override
    public String type() {
        return "kval";
    }

    @Override
    public String extract(HttpResponseContext ctx, ExtractorDef def) {
        List<String> keys = def.getKval();
        if (keys == null || keys.isEmpty() || ctx.getHeaders() == null) {
            return null;
        }
        /* 遍历键名，查找第一个存在的键并返回其第一个值 */
        for (String key : keys) {
            if (ctx.getHeaders().containsKey(key)) {
                List<String> values = ctx.getHeaders().get(key);
                if (values != null && !values.isEmpty()) {
                    return values.get(0);
                }
            }
        }
        return null;
    }
}
