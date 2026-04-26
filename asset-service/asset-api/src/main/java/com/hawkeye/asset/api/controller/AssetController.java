package com.hawkeye.asset.api.controller;

import com.common.utils.response.ApiResponse;
import com.common.utils.response.ListResult;
import com.hawkeye.asset.business.mapstruct.AssetMapstruct;
import com.hawkeye.asset.business.service.AssetService;
import com.hawkeye.asset.common.pojo.DTO.AssetPageQueryDTO;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 资产控制层
 */
@RestController
@RequestMapping("/asset")
@RequiredArgsConstructor
public class AssetController {
    private final AssetService assetService;
    private final AssetMapstruct assetMapstruct;

    @GetMapping
    public ApiResponse<ListResult<PageAssetVO.Response>> pageQuery(PageAssetVO.Request request) {
        AssetPageQueryDTO assetPageQueryDTO = assetMapstruct.toAssetPageQueryDTO(request);
        return ApiResponse.success(assetService.pageQuery(assetPageQueryDTO));
    }
}
