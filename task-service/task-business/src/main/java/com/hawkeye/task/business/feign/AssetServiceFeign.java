package com.hawkeye.task.business.feign;

import com.common.utils.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * asset-service 内部 Feign — 获取资产信息。
 */
@FeignClient(name = "asset-service", path = "/asset")
public interface AssetServiceFeign {

    @GetMapping("/{assetId}")
    ApiResponse<Map<String, Object>> getAsset(@PathVariable("assetId") Long assetId);
}
