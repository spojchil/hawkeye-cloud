package com.hawkeye.asset.common.pojo.vo.asset;

import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class AssetVO {

    @Data
    public static class Request {
        @NotBlank(message = "资产名称不能为空")
        private String name;

        @NotBlank(message = "协议不能为空")
        private String requestProtocol;

        @NotBlank(message = "请求主机不能为空")
        private String requestHost;

        @NotNull(message = "请求端口不能为空")
        private Integer requestPort;

        private String requestPath;
        private String description;
        private AssetStatusEnum status;
        private AssetRiskEnum riskLevel;
    }

    @Data
    public static class Response {
        private Long assetId;
        private String name;
        private String requestProtocol;
        private String requestHost;
        private Integer requestPort;
        private String requestPath;
        private String description;
        private AssetStatusEnum status;
        private AssetRiskEnum riskLevel;
        private LocalDateTime lastScanTime;
    }
}
