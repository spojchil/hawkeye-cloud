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

@Tag(name = "漏洞分类管理", description = "漏洞模板分类树管理")
@RestController
@RequestMapping("/vul-category")
@RequiredArgsConstructor
public class VulCategoryController {

    private final VulCategoryService vulCategoryService;

    @Operation(summary = "查询分类列表")
    @GetMapping
    public ApiResponse<List<VulCategoryVO.Response>> listCategories(
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) String name) {
        return ApiResponse.success(vulCategoryService.listCategories(parentId, name));
    }

    @Operation(summary = "创建分类")
    @PostMapping
    public ApiResponse<VulCategoryVO.Response> create(@Valid @RequestBody VulCategoryVO.Request request) {
        return ApiResponse.success(vulCategoryService.create(request));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public ApiResponse<VulCategoryVO.Response> update(@PathVariable Long id,
                                                       @Valid @RequestBody VulCategoryVO.Request request) {
        return ApiResponse.success(vulCategoryService.update(id, request));
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        vulCategoryService.delete(id);
        return ApiResponse.success(null);
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
