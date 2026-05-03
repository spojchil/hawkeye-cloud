package com.hawkeye.detection.common.pojo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * RocketMQ 消息体 — 单条 (资产 × 模板) 检测组合。
 * <p>
 * 携带检测执行所需的全部数据：资产信息 + 模板检测配置。
 * detection-service 不调 Feign，拿到消息直接执行。
 * <p>
 * 消息体大小估算：单模板平均 ~1.5KB JSON，5000 条 ≈ 7.5MB。
 */
@Data
public class TaskItemMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── 任务标识 ──
    private Long taskId;
    private Long itemId;
    private Long tenantId;
    private Long createdAt;

    // ── 资产信息（变量解析用） ──
    private String assetProtocol;       // http / https
    private String assetHost;           // example.com
    private Integer assetPort;          // 443
    private String assetPath;           // /api

    // ── 模板检测配置 ──
    private String templateId;          // YAML 业务 ID（日志追溯）
    private Long templateDbId;          // vul_template.template_id（DB 主键）
    private String flow;                // 执行流表达式，null=单步
    private Map<String, Object> variables;   // 模板级动态变量
    private List<HttpStep> httpSteps;   // HTTP 请求步骤

    // ── 内嵌 DTO ──

    @Data
    public static class HttpStep {
        private Integer stepOrder;
        private String method;
        private List<String> path;
        private Map<String, String> headers;
        private String body;
        private String raw;
        private String attack;
        private String matchersCondition;
        private List<Matcher> matchers;
        private List<Extractor> extractors;
    }

    @Data
    public static class Matcher {
        private String type;
        private String part;
        private String condition;
        private Boolean negative;
        private Boolean caseInsensitive;
        private Map<String, Object> config;
    }

    @Data
    public static class Extractor {
        private String type;
        private String part;
        private String name;
        private Map<String, Object> config;
        private Boolean internal;
        private Integer groupNum;
    }
}
