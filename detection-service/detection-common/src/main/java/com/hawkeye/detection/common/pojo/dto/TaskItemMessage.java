package com.hawkeye.detection.common.pojo.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * RocketMQ 消息体 — 单条 (资产 × 模板) 检测组合。
 * <p>
 * 由 task-service 构建，通过 RocketMQ 发送到 detection-service。
 * 携带检测执行所需的全部数据：资产信息 + 模板检测配置。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>消息体携带全量数据，detection-service 不调 Feign</li>
 *   <li>消息体大小估算：单模板平均 ~1.5KB JSON，5000 条 ≈ 7.5MB</li>
 *   <li>消息体不可变，消费后无需修改</li>
 * </ul>
 * <p>
 * 字段说明：
 * <ul>
 *   <li>任务标识 - taskId、itemId、assetId、tenantId</li>
 *   <li>资产信息 - protocol、host、port、path（用于构建 BaseURL）</li>
 *   <li>模板配置 - templateId、flow、variables、httpSteps</li>
 * </ul>
 */
@Data
public class TaskItemMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── 任务标识 ──────────────────────────────────────────────────────

    /** 任务 ID */
    private Long taskId;

    /** 检测项 ID */
    private Long itemId;

    /** 资产 ID */
    private Long assetId;

    /** 租户 ID */
    private Long tenantId;

    /** 消息创建时间戳 */
    private Long createdAt;

    // ── 资产信息（VariableContext 构建 BaseURL 用）─────────────────────

    /** 协议：http / https */
    private String assetProtocol;

    /** 主机名：example.com */
    private String assetHost;

    /** 端口号：80、443、8080 等 */
    private Integer assetPort;

    /** 路径：/api */
    private String assetPath;

    // ── 模板检测配置 ──────────────────────────────────────────────────

    /** YAML 业务 ID（日志追溯） */
    private String templateId;

    /** 模板 DB 主键（vul_template.template_id） */
    private Long templateDbId;

    /** 执行流表达式，null 表示单步 */
    private String flow;

    /** 模板级动态变量 */
    private Map<String, Object> variables;

    /** HTTP 请求步骤列表 */
    private List<HttpStep> httpSteps;

    // ── 内嵌 DTO ──────────────────────────────────────────────────────

    /**
     * HTTP 步骤定义。
     */
    @Data
    public static class HttpStep {
        /** 步骤顺序（1-based） */
        private Integer stepOrder;
        /** HTTP 方法 */
        private String method;
        /** 请求路径列表（支持多路径尝试） */
        private List<String> path;
        /** 请求头 */
        private Map<String, String> headers;
        /** 请求体 */
        private String body;
        /** 原始 HTTP 文本（raw 模式） */
        private String raw;
        /** 攻击模式 */
        private String attack;
        /** 匹配器条件（and/or） */
        private String matchersCondition;
        /** 匹配器列表 */
        private List<Matcher> matchers;
        /** 提取器列表 */
        private List<Extractor> extractors;
    }

    /**
     * 匹配器定义。
     */
    @Data
    public static class Matcher {
        /** 匹配器类型（word/status/regex/dsl） */
        private String type;
        /** 匹配目标部分（body/header/all） */
        private String part;
        /** 条件（and/or） */
        private String condition;
        /** 是否取反 */
        private Boolean negative;
        /** 是否忽略大小写 */
        private Boolean caseInsensitive;
        /** 配置（words/status/regex/dsl 等） */
        private Map<String, Object> config;
    }

    /**
     * 提取器定义。
     */
    @Data
    public static class Extractor {
        /** 提取器类型（regex/kval/json/dsl） */
        private String type;
        /** 提取源（body/header） */
        private String part;
        /** 提取变量名 */
        private String name;
        /** 配置（regex/kval/json/dsl 等） */
        private Map<String, Object> config;
        /** 是否仅内部传递 */
        private Boolean internal;
        /** 正则捕获组编号 */
        private Integer groupNum;
    }
}
