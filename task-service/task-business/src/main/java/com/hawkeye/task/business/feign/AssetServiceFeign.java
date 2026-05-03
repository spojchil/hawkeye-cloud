package com.hawkeye.task.business.feign;

import com.common.utils.response.ApiResponse;
import com.hawkeye.asset.common.pojo.vo.asset.AssetVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "asset-service", path = "/asset")
public interface AssetServiceFeign {

    @GetMapping("/{assetId}")
    ApiResponse<AssetVO.Response> getAsset(@PathVariable Long assetId);
}
