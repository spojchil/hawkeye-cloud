package com.hawkeye.vul.api.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.common.utils.response.ApiResponse;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.vul.business.service.VulTemplateService;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateDetailVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplatePageQueryVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplatePageVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateRequestVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "漏洞模板管理")
@RestController
@RequestMapping("/vul")
@RequiredArgsConstructor
public class VulTemplateController {

    private final VulTemplateService vulTemplateService;

    @Operation(summary = "分页查询模板")
    @GetMapping
    public ApiResponse<IPage<VulTemplatePageVO>> pageQuery(@ParameterObject @Valid VulTemplatePageQueryVO query) {
        return ApiResponse.success(vulTemplateService.pageQuery(query));
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    public ApiResponse<VulTemplateDetailVO> getDetail(@PathVariable Long id) {
        return ApiResponse.success(vulTemplateService.getDetail(id));
    }

    @Operation(summary = "创建模板")
    @PostMapping
    public ApiResponse<Long> create(@Valid @RequestBody VulTemplateRequestVO request) {
        return ApiResponse.success(vulTemplateService.create(request));
    }

    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody VulTemplateRequestVO request) {
        vulTemplateService.update(id, request);
        return ApiResponse.success();
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        vulTemplateService.delete(id);
        return ApiResponse.success();
    }

    @Operation(summary = "启用/禁用模板")
    @PatchMapping("/{id}/enabled")
    public ApiResponse<Void> setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        vulTemplateService.setEnabled(id, body.get("enabled"));
        return ApiResponse.success();
    }

    @Operation(summary = "批量删除模板")
    @PostMapping("/batch-delete")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        vulTemplateService.batchDelete(ids);
        return ApiResponse.success();
    }
}
