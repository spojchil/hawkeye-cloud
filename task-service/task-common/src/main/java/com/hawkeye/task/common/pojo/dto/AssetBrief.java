package com.hawkeye.task.common.pojo.dto;

import lombok.Data;

/**
 * task-service 本地 Feign 响应 DTO（不依赖 asset-common）。
 * 从 GET /asset/{id} 的 JSON 反序列化，只包含检测调度需要的字段。
 */
@Data
public class AssetBrief {
    private String requestProtocol;
    private String requestHost;
    private Integer requestPort;
    private String requestPath;
}
