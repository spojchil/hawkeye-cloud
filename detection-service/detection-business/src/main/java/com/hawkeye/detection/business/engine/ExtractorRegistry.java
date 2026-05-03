package com.hawkeye.detection.business.engine;

import com.hawkeye.detection.business.engine.extractor.Extractor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

@Component
public class ExtractorRegistry {
    private final Map<String, Extractor> map;

    public ExtractorRegistry(List<Extractor> list) {
        this.map = list.stream().collect(toMap(Extractor::type, identity()));
    }

    public Extractor get(String type) {
        Extractor e = map.get(type);
        if (e == null) throw new IllegalArgumentException("不支持的提取器类型: " + type);
        return e;
    }
}
