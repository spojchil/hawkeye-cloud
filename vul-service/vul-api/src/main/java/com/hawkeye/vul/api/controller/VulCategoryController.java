package com.hawkeye.vul.api.controller;

import com.common.utils.response.ApiResponse;
import com.hawkeye.vul.business.service.VulCategoryService;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "漏洞分类管理")
@RestController
@RequestMapping("/vul-category")
@RequiredArgsConstructor
public class VulCategoryController {

    private final VulCategoryService vulCategoryService;

    @GetMapping
    @Operation(summary = "查询分类树")
    public ApiResponse<List<VulCategoryVO.Response>> tree(
            @RequestParam(required = false) Long parentId) {
        return ApiResponse.success(vulCategoryService.tree(parentId));
    }

    @PostMapping
    @Operation(summary = "创建分类")
    public ApiResponse<VulCategoryVO.Response> create(
            @RequestBody @Valid VulCategoryVO.Request request) {
        return ApiResponse.success(vulCategoryService.create(request));
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "更新分类")
    public ApiResponse<VulCategoryVO.Response> update(@PathVariable Long categoryId,
                                                       @RequestBody VulCategoryVO.Request request) {
        return ApiResponse.success(vulCategoryService.update(categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    @Operation(summary = "删除分类")
    public ApiResponse<Void> delete(@PathVariable Long categoryId) {
        vulCategoryService.delete(categoryId);
        return ApiResponse.success();
    }

    @PostMapping("/{categoryId}/templates")
    @Operation(summary = "给分类批量添加模板")
    public ApiResponse<Integer> addTemplates(@PathVariable Long categoryId,
                                              @RequestBody List<Long> templateIds) {
        return ApiResponse.success(vulCategoryService.addTemplates(categoryId, templateIds));
    }

    @DeleteMapping("/{categoryId}/templates")
    @Operation(summary = "从分类批量移除模板")
    public ApiResponse<Integer> removeTemplates(@PathVariable Long categoryId,
                                                 @RequestBody List<Long> templateIds) {
        return ApiResponse.success(vulCategoryService.removeTemplates(categoryId, templateIds));
    }
}
