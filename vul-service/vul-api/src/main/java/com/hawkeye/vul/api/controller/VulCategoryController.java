package com.hawkeye.vul.api.controller;

import com.common.utils.response.ApiResponse;
import com.hawkeye.vul.business.service.VulCategoryService;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryVO;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryRequestVO;
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

    @Operation(summary = "查询分类树")
    @GetMapping
    public ApiResponse<List<VulCategoryVO>> tree(@RequestParam(required = false) Long parentId) {
        return ApiResponse.success(vulCategoryService.tree(parentId));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody VulCategoryRequestVO request) {
        return ApiResponse.success(vulCategoryService.create(request));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody VulCategoryRequestVO request) {
        vulCategoryService.update(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        vulCategoryService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "给分类批量添加模板")
    @PostMapping("/{id}/templates")
    public ApiResponse<Integer> addTemplates(@PathVariable Long id, @RequestBody List<Long> templateIds) {
        return ApiResponse.success(vulCategoryService.addTemplates(id, templateIds));
    }

    @Operation(summary = "从分类批量移除模板")
    @DeleteMapping("/{id}/templates")
    public ApiResponse<Integer> removeTemplates(@PathVariable Long id, @RequestBody List<Long> templateIds) {
        return ApiResponse.success(vulCategoryService.removeTemplates(id, templateIds));
    }
}
