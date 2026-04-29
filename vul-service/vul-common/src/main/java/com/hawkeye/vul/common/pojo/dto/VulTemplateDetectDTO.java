package com.hawkeye.vul.common.pojo.dto;

import lombok.Data;

/**
 * 给 detection-service 使用的模板检测数据。
 * 仅包含执行检测所需的字段，不传输 name/description 等元数据。
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
