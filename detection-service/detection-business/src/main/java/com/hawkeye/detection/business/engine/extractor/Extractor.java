package com.hawkeye.detection.business.engine.extractor;

import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;

/**
 * 提取器接口——type() 返回提取器类型标识，extract() 执行提取并返回结果
 */
public interface Extractor {

    /**
     * 提取器类型标识。
     *
     * @return 类型标识（如 "regex"、"kval"）
     */
    String type();

    /**
     * 从 HTTP 响应中提取变量值。
     *
     * @param ctx HTTP 响应上下文
     * @param def 提取器定义
     * @return 提取的变量值，如果未提取到返回 null
     */
    String extract(HttpResponseContext ctx, ExtractorDef def);
}
