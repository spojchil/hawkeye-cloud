package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;

/**
 * 匹配器策略接口。每种 type 一个 @Component 实现，由 MatcherRegistry 自动收集。
 */
public interface Matcher {

    /** 匹配器类型，对应 vul_matcher.type */
    String type();

    /** 对 HTTP 响应执行匹配 */
    boolean match(HttpResponseContext ctx, MatcherDef def);
}
