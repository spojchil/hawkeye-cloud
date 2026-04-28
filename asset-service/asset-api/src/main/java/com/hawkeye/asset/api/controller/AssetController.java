package com.hawkeye.asset.api.controller;

import com.common.utils.response.ApiResponse;
import com.common.utils.response.ListResult;
import com.hawkeye.asset.business.service.AssetService;
import com.hawkeye.asset.common.pojo.vo.asset.AssetVO;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/asset")
@RequiredArgsConstructor
@Tag(name = "资产管理")
public class AssetController {
    private final AssetService assetService;

    @GetMapping
    @Operation(summary = "分页查询资产")
    public ApiResponse<ListResult<PageAssetVO.Response>> pageQuery(@ParameterObject PageAssetVO.Request request) {
        return ApiResponse.success(assetService.pageQuery(request));
    }

    @GetMapping("/{assetId}")
    @Operation(summary = "获取资产详情")
    public ApiResponse<AssetVO.Response> getById(@PathVariable Long assetId) {
        return ApiResponse.success(assetService.getById(assetId));
    }

    @PostMapping
    @Operation(summary = "创建资产")
    public ApiResponse<AssetVO.Response> create(@RequestBody @Valid AssetVO.Request request) {
        return ApiResponse.success(assetService.create(request));
    }

    @PutMapping("/{assetId}")
    @Operation(summary = "更新资产")
    public ApiResponse<AssetVO.Response> update(@PathVariable Long assetId,
                                                 @RequestBody AssetVO.Request request) {
        return ApiResponse.success(assetService.update(assetId, request));
    }

    @DeleteMapping("/{assetId}")
    @Operation(summary = "删除资产")
    public ApiResponse<Void> delete(@PathVariable Long assetId) {
        assetService.delete(assetId);
        return ApiResponse.success();
    }
}
