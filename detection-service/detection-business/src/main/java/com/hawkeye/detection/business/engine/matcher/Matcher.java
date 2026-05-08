package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;

/**
 * 匹配器接口——type() 返回匹配器类型标识，match() 执行匹配判定
 */
public interface Matcher {

    /**
     * 匹配器类型标识。
     * <p>
     * 对应数据库 vul_matcher.type 字段。
     *
     * @return 类型标识（如 "word"、"status"、"regex"、"dsl"）
     */
    String type();

    /**
     * 对 HTTP 响应执行匹配。
     *
     * @param ctx HTTP 响应上下文
     * @param def 匹配器定义
     * @return true 如果匹配成功
     */
    boolean match(HttpResponseContext ctx, MatcherDef def);
}
