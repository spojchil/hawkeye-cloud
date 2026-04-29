package com.hawkeye.detection.business.feign;

import com.common.utils.response.ApiResponse;
import com.hawkeye.detection.common.pojo.dto.AssetDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * asset-service 内部 Feign 客户端。
 */
@FeignClient(name = "asset-service", path = "/asset")
public interface AssetServiceFeign {

    @GetMapping("/internal/{assetId}")
    ApiResponse<AssetDTO> getAsset(@PathVariable("assetId") Long assetId);
}
