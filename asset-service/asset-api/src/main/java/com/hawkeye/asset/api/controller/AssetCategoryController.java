package com.hawkeye.asset.api.controller;

import com.common.utils.response.ApiResponse;
import com.hawkeye.asset.business.service.AssetCategoryService;
import com.hawkeye.asset.common.pojo.vo.category.CategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/asset-category")
@RequiredArgsConstructor
@Tag(name = "资产分类管理")
public class AssetCategoryController {

    private final AssetCategoryService assetCategoryService;

    @GetMapping
    @Operation(summary = "查询分类列表")
    public ApiResponse<List<CategoryVO.Response>> listCategories(
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String name) {
        return ApiResponse.success(assetCategoryService.listCategories(parentId, name));
    }

    @PostMapping
    @Operation(summary = "创建分类")
    public ApiResponse<CategoryVO.Response> create(@RequestBody @Valid CategoryVO.Request request) {
        return ApiResponse.success(assetCategoryService.create(request));
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "更新分类")
    public ApiResponse<CategoryVO.Response> update(@PathVariable Long categoryId,
                                                    @RequestBody CategoryVO.Request request) {
        return ApiResponse.success(assetCategoryService.update(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    @Operation(summary = "删除分类")
    public ApiResponse<Void> delete(@PathVariable Long categoryId) {
        assetCategoryService.delete(categoryId);
        return ApiResponse.success();
    }

    @PostMapping("/{categoryId}/assets")
    @Operation(summary = "给分类添加资产")
    public ApiResponse<Integer> addAssets(@PathVariable Long categoryId,
                                           @RequestBody List<Long> assetIds) {
        return ApiResponse.success(assetCategoryService.addAssets(categoryId, assetIds));
    }

    @DeleteMapping("/{categoryId}/assets")
    @Operation(summary = "从分类移除资产")
    public ApiResponse<Integer> removeAssets(@PathVariable Long categoryId,
                                              @RequestBody List<Long> assetIds) {
        return ApiResponse.success(assetCategoryService.removeAssets(categoryId, assetIds));
    }
}
