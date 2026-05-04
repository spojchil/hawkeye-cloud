package com.hawkeye.detection.business.engine.matcher;

import com.hawkeye.detection.business.engine.model.HttpResponseContext;
import com.hawkeye.detection.business.engine.model.MatcherDef;

/**
 * 匹配器策略接口。
 * <p>
 * 每种匹配类型（word、status、dsl、regex 等）都需要实现此接口，
 * 并使用 @Component 注解注册为 Spring Bean。
 * <p>
 * MatcherRegistry 会自动收集所有 Matcher 实现，按 type 注册。
 * <p>
 * 实现示例：
 * <pre>
 * @Component
 * public class WordMatcher extends AbstractMatcher {
 *     @Override
 *     public String type() { return "word"; }
 *
 *     @Override
 *     public boolean match(HttpResponseContext ctx, MatcherDef def) {
 *         String target = part(ctx, def.getPart());
 *         return target != null && target.contains(def.getWords().get(0));
 *     }
 * }
 * </pre>
 * <p>
 * 支持的匹配类型：
 * <ul>
 *   <li>word - 关键词匹配</li>
 *   <li>status - HTTP 状态码匹配</li>
 *   <li>regex - 正则表达式匹配</li>
 *   <li>dsl - DSL 表达式匹配</li>
 * </ul>
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
