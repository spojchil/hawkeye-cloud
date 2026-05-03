package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;

/** 提取器定义 — 编译期类型安全 */
@Data
public class ExtractorDef {
    private String type;              // regex / kval / json / dsl / xpath
    private String part;              // body / header
    private String name;              // 提取变量名
    private boolean internal;
    private Integer groupNum;

    private List<String> regex;
    private List<String> kval;
    private List<String> json;
    private List<String> dsl;
    private List<String> xpath;
}
