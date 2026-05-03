package com.hawkeye.detection.business.engine.extractor;

import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;

/** 提取器策略接口。每种 type 一个 @Component 实现，由 ExtractorRegistry 自动收集。 */
public interface Extractor {

    String type();

    /** 从 HTTP 响应中提取变量值，返回 null 表示未提取到 */
    String extract(HttpResponseContext ctx, ExtractorDef def);
}
