package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.matcher.Matcher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * 匹配器注册表 — Spring 自动注入所有 Matcher 实现。
 */
@Component
public class MatcherRegistry {

    private final Map<String, Matcher> map;

    public MatcherRegistry(List<Matcher> list) {
        this.map = list.stream().collect(toMap(Matcher::type, identity()));
    }

    public Matcher get(String type) {
        Matcher m = map.get(type);
        if (m == null) throw new IllegalArgumentException("不支持的匹配器类型: " + type);
        return m;
    }
}
