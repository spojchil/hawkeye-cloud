package com.hawkeye.detection.common.pojo.dto;

import lombok.Data;

/**
 * 资产信息 DTO（从 asset-service Feign 获取）。
 * 仅包含构造 HTTP 请求所需的字段。
 */
@Data
public class AssetDTO {
    private Long assetId;
    private String requestProtocol;
    private String requestHost;
    private Integer requestPort;
    private String requestPath;
    private String status;
}
