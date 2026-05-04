package com.hawkeye.detection.business.engine.extractor;

import com.hawkeye.detection.business.engine.model.ExtractorDef;
import com.hawkeye.detection.business.engine.model.HttpResponseContext;

/**
 * 提取器策略接口。
 * <p>
 * 每种提取类型（regex、kval、json、dsl 等）都需要实现此接口，
 * 并使用 @Component 注解注册为 Spring Bean。
 * <p>
 * ExtractorRegistry 会自动收集所有 Extractor 实现，按 type 注册。
 * <p>
 * 实现示例：
 * <pre>
 * @Component
 * public class RegexExtractor implements Extractor {
 *     @Override
 *     public String type() { return "regex"; }
 *
 *     @Override
 *     public String extract(HttpResponseContext ctx, ExtractorDef def) {
 *         // 从响应中提取变量值
 *         return extractedValue;
 *     }
 * }
 * </pre>
 * <p>
 * 支持的提取类型：
 * <ul>
 *   <li>regex - 正则表达式提取</li>
 *   <li>kval - 键值提取（从响应头提取）</li>
 *   <li>json - JSON 路径提取</li>
 *   <li>dsl - DSL 表达式提取</li>
 * </ul>
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
