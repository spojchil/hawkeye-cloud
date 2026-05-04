package com.hawkeye.task.business.feign;

import com.common.utils.response.ApiResponse;
import com.hawkeye.task.common.pojo.dto.AssetBrief;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 资产服务 Feign 客户端。
 */
@FeignClient(name = "asset-service", path = "/asset")
public interface AssetServiceFeign {

    /** 查询资产基本信息 */
    @GetMapping("/{assetId}")
    ApiResponse<AssetBrief> getAsset(@PathVariable Long assetId);
}
