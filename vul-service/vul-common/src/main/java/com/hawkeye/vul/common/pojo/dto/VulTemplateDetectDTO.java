package com.hawkeye.vul.common.pojo.dto;

import lombok.Data;

/**
 * 给 detection-service 使用的模板检测数据。
 * 仅包含执行检测所需的字段，不传输 name/description 等元数据。
 * <p>
 * 注意：{@code templateId} 是 YAML 业务 ID（如 "CVE-2024-0012"），
 * 不是数据库自增主键 {@code id}。detection-service 消费时应按 templateId 查找模板。
 */
@Data
public class VulTemplateDetectDTO {
    private String templateId;
    private String flow;
    private String variables;
    private String httpRequests;
    private String matchers;
    private String extractors;
}
