package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.extractor.Extractor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * 提取器注册表。
 * <p>
 * Spring 自动注入所有 Extractor 实现，按 type 注册到 Map 中。
 * <p>
 * 工作原理：
 * <ul>
 *   <li>构造函数接收 Spring 注入的 List<Extractor></li>
 *   <li>按 Extractor::type 构建 Map</li>
 *   <li>get(type) 方法返回对应的 Extractor 实现</li>
 * </ul>
 * <p>
 * 新增提取器只需：
 * <ol>
 *   <li>实现 Extractor 接口</li>
 *   <li>添加 @Component 注解</li>
 *   <li>Spring 会自动注入到此注册表</li>
 * </ol>
 */
@Component
public class ExtractorRegistry {

    /** 提取器映射表：type → Extractor */
    private final Map<String, Extractor> map;

    /**
     * 构造函数 — Spring 自动注入所有 Extractor 实现。
     *
     * @param list 所有 Extractor 实现列表
     */
    public ExtractorRegistry(List<Extractor> list) {
        this.map = list.stream().collect(toMap(Extractor::type, identity()));
    }

    /**
     * 根据类型获取提取器。
     *
     * @param type 提取器类型（如 "regex"、"kval"）
     * @return 提取器实例
     * @throws IllegalArgumentException 如果类型不存在
     */
    public Extractor get(String type) {
        Extractor e = map.get(type);
        if (e == null) {
            throw new IllegalArgumentException("不支持的提取器类型: " + type);
        }
        return e;
    }
}
