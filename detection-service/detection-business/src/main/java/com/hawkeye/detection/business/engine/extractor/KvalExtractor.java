package com.hawkeye.detection.business.engine.extractor;

import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 键值提取器。
 * <p>
 * 从 HTTP 响应头中提取指定键的值。
 * <p>
 * 使用场景：
 * <ul>
 *   <li>提取 Set-Cookie 中的 session ID</li>
 *   <li>提取 Location 中的重定向 URL</li>
 *   <li>提取自定义响应头中的令牌</li>
 * </ul>
 * <p>
 * 配置示例：
 * <pre>
 * {
 *   "type": "kval",
 *   "part": "header",
 *   "name": "session_id",
 *   "kval": ["Set-Cookie", "X-Session-Id"]
 * }
 * </pre>
 * <p>
 * 提取逻辑：
 * <ol>
 *   <li>遍历 kval 列表中的键名</li>
 *   <li>在响应头中查找第一个存在的键</li>
 *   <li>返回该键对应的第一个值</li>
 * </ol>
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

        // 遍历键名，查找第一个存在的键
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
