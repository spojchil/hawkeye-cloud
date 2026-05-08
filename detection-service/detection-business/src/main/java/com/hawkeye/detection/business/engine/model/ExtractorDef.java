package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;

/**
 * 提取器定义——每种类型只读自己字段，其他为 null，编译期类型安全
 */
@Data
public class ExtractorDef {

    /** 提取器类型：regex / kval / json / dsl / xpath */
    private String type;

    /** 提取源：body / header */
    private String part;

    /** 提取变量名（写入 VariableContext 的 key） */
    private String name;

    /** 是否仅内部传递 */
    private boolean internal;

    /** 正则捕获组编号（仅 regex 类型） */
    private Integer groupNum;

    /* type-specific（策略内部按 type 取用） */

    /** regex 类型的正则表达式列表 */
    private List<String> regex;

    /** kval 类型的键名列表 */
    private List<String> kval;

    /** json 类型的 JSON 路径列表 */
    private List<String> json;

    /** dsl 类型的表达式列表 */
    private List<String> dsl;

    /** xpath 类型的 XPath 表达式列表 */
    private List<String> xpath;
}
