package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;

/**
 * 提取器定义 — 编译期类型安全。
 * <p>
 * 设计思路与 MatcherDef 相同：
 * <ul>
 *   <li>每种提取器类型只读自己关心的字段</li>
 *   <li>其他字段为 null</li>
 *   <li>编译期检查，IDE 补全友好</li>
 * </ul>
 * <p>
 * 字段说明：
 * <ul>
 *   <li>type - 提取器类型（regex/kval/json/dsl/xpath）</li>
 *   <li>part - 提取源（body/header）</li>
 *   <li>name - 提取的变量名（写入 VariableContext 的 key）</li>
 *   <li>internal - 是否仅内部传递</li>
 *   <li>groupNum - 正则捕获组编号</li>
 *   <li>regex/kval/json/dsl/xpath - 各类型对应的配置</li>
 * </ul>
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

    // ── type-specific（策略内部按 type 取用）─────────────────────────

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
