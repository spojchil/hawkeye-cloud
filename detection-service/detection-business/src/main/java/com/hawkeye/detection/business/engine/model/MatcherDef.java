package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;

/**
 * 匹配器定义——每种类型只读自己字段，其他为 null，编译期类型安全
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

    /* type-specific（策略内部按 type 取用） */

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
