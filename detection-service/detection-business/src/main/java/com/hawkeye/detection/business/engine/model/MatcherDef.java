package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;

/**
 * 匹配器定义 — 胖 DTO，编译期类型安全。
 * <p>
 * 设计思路：
 * <ul>
 *   <li>每种匹配器类型（word/status/regex/dsl）只读自己关心的字段</li>
 *   <li>其他字段为 null，避免使用 Map 带来的类型转换问题</li>
 *   <li>编译期检查，IDE 补全友好</li>
 * </ul>
 * <p>
 * 字段说明：
 * <ul>
 *   <li>type - 匹配器类型（word/status/regex/dsl）</li>
 *   <li>part - 匹配目标（body/header/all）</li>
 *   <li>condition - 多条规则的组合条件（and/or）</li>
 *   <li>negative - 是否取反</li>
 *   <li>caseInsensitive - 是否忽略大小写</li>
 *   <li>words/status/dsl/regex - 各类型对应的规则列表</li>
 * </ul>
 */
@Data
public class MatcherDef {

    /** 匹配器类型：word / status / dsl / regex / binary / size / xpath */
    private String type;

    /** 匹配目标部分：body / header / all / content_type */
    private String part;

    /** 单 matcher 内多条规则的关系：and / or */
    private String condition;

    /** 是否取反 */
    private boolean negative;

    /** 是否忽略大小写 */
    private boolean caseInsensitive;

    /** 是否匹配所有值 */
    private boolean matchAll;

    /** 是否仅内部使用 */
    private boolean internal;

    /** 匹配器名称 */
    private String name;

    // ── type-specific（策略内部按 type 取用）─────────────────────────

    /** word 类型的关键词列表 */
    private List<String> words;

    /** status 类型的状态码列表 */
    private List<Integer> status;

    /** dsl 类型的表达式列表 */
    private List<String> dsl;

    /** regex 类型的正则表达式列表 */
    private List<String> regex;

    /** binary 类型的二进制模式列表 */
    private List<String> binary;

    /** size 类型的大小列表 */
    private List<Integer> size;

    /** xpath 类型的 XPath 表达式列表 */
    private List<String> xpath;

    /**
     * 根据 type 返回实际规则列表。
     *
     * @return 规则列表
     */
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
