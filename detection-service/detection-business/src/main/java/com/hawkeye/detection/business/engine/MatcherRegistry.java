package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.matcher.Matcher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * 匹配器注册表。
 * <p>
 * Spring 自动注入所有 Matcher 实现，按 type 注册到 Map 中。
 * <p>
 * 工作原理：
 * <ul>
 *   <li>构造函数接收 Spring 注入的 List<Matcher></li>
 *   <li>按 Matcher::type 构建 Map</li>
 *   <li>get(type) 方法返回对应的 Matcher 实现</li>
 * </ul>
 * <p>
 * 新增匹配器只需：
 * <ol>
 *   <li>实现 Matcher 接口</li>
 *   <li>添加 @Component 注解</li>
 *   <li>Spring 会自动注入到此注册表</li>
 * </ol>
 */
@Component
public class MatcherRegistry {

    /** 匹配器映射表：type → Matcher */
    private final Map<String, Matcher> map;

    /**
     * 构造函数 — Spring 自动注入所有 Matcher 实现。
     *
     * @param list 所有 Matcher 实现列表
     */
    public MatcherRegistry(List<Matcher> list) {
        this.map = list.stream().collect(toMap(Matcher::type, identity()));
    }

    /**
     * 根据类型获取匹配器。
     *
     * @param type 匹配器类型（如 "word"、"status"、"regex"、"dsl"）
     * @return 匹配器实例
     * @throws IllegalArgumentException 如果类型不存在
     */
    public Matcher get(String type) {
        Matcher m = map.get(type);
        if (m == null) {
            throw new IllegalArgumentException("不支持的匹配器类型: " + type);
        }
        return m;
    }
}
