package com.hawkeye.detection.business.engine.model;

import lombok.Data;

import java.util.List;

/**
 * 单条 matcher 配置（从 matchers JSON 解析）。
 */
@Data
public class MatcherConfig {
    private String type;
    private String part;
    private List<String> words;
    private List<Integer> status;
    private List<String> dsl;
    private List<String> regex;
    private String condition;
    private boolean caseInsensitive;
    private boolean negative;
}
