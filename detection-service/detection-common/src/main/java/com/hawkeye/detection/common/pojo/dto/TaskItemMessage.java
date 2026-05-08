package com.hawkeye.detection.common.pojo.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * RocketMQ 消息体——包含全量执行数据，detection 消费后零外部依赖直接执行
 */
@Data
public class TaskItemMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private Long itemId;
    private Long assetId;
    private Long tenantId;
    private Long createdAt;

    /* 资产信息 */
    private String assetProtocol;
    private String assetHost;
    private Integer assetPort;
    private String assetPath;

    /* 模板检测配置 */
    private String templateId;
    private Long templateDbId;
    private String flow;
    private Map<String, Object> variables;
    private List<HttpStep> httpSteps;

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
