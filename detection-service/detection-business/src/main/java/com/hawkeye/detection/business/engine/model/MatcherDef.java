package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;

/**
 * 匹配器定义 — 胖 DTO，编译期类型安全。
 * 每种策略只读自己 type 对应的字段，其余为 null。
 */
@Data
public class MatcherDef {

    /** word / status / dsl / regex / binary / size / xpath */
    private String type;

    /** body / header / all / content_type / ... */
    private String part;

    /** 单 matcher 内多条规则的关系: and / or */
    private String condition;

    private boolean negative;

    private boolean caseInsensitive;

    private boolean matchAll;

    private boolean internal;

    private String name;

    // ── type-specific（策略内部按 type 取用）──

    private List<String> words;
    private List<Integer> status;
    private List<String> dsl;
    private List<String> regex;
    private List<String> binary;
    private List<Integer> size;
    private List<String> xpath;

    /** 根据 type 返回实际规则列表 */
    public List<?> rules() {
        return switch (type) {
            case "word" -> words;
            case "status" -> status;
            case "dsl" -> dsl;
            case "regex" -> regex;
            case "binary" -> binary;
            case "size" -> size;
            case "xpath" -> xpath;
            default -> null;
        };
    }
}
