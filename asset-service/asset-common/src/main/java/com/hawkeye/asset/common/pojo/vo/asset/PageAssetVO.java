package com.hawkeye.asset.common.pojo.vo.asset;

import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import lombok.Data;

public class PageAssetVO {
    @Data
    public static class Request{
        private Integer page;
        private Integer pageSize;
        private String name;
        private String requestHost;
        private AssetRiskEnum riskLevel;
        private AssetStatusEnum status;
        private Long categoryId;
    }

    public static class Response{
        private Long assetId;
        private String name;
        private String requestHost;
        private AssetRiskEnum riskLevel;
        private AssetStatusEnum status;
    }
}
