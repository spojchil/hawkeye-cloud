package com.hawkeye.asset.common.pojo.DTO;

import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import com.hawkeye.asset.common.enums.RequestMethodEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 资产DTO
 */
@Data
public class AssetDTO {
    private Long assetId;
    private String name;
    private RequestMethodEnum requestMethod;
    private String requestProtocol;
    private String requestHost;
    private Integer requestPort;
    private String requestPath;
    private String requestHeader;
    private String description;
    private AssetStatusEnum status;
    private AssetRiskEnum riskLevel;
    private LocalDateTime lastScanTime;
}
