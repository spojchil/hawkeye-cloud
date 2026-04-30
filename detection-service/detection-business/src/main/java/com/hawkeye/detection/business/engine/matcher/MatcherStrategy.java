package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherConfig;

/**
 * 匹配器策略接口。
 */
public interface MatcherStrategy {

    /** 支持的 matcher type */
    String getType();

    /**
     * 判定单条规则是否匹配。
     * MatcherChain 负责处理 and/or/negative 逻辑。
     */
    boolean matchRule(HttpResponseContext ctx, MatcherConfig cfg, Object rule);
}
