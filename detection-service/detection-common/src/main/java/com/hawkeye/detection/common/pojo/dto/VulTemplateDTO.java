package com.hawkeye.detection.common.pojo.dto;

import lombok.Data;

/**
 * 漏洞模板检测数据 DTO（从 vul-service Feign 获取）。
 * 与 vul-service 的 VulTemplateDetectDTO 结构一致。
 */
@Data
public class VulTemplateDTO {
    private String templateId;
    private String flow;
    private String variables;
    private String httpRequests;
    private String matchers;
    private String extractors;
}
