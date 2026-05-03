package com.hawkeye.vul.api.controller;

import com.common.utils.response.ApiResponse;
import com.common.utils.response.ListResult;
import com.hawkeye.vul.business.service.VulTemplateService;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateImportVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplatePageVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@Tag(name = "漏洞模板管理")
@RestController
@RequestMapping("/vul")
@RequiredArgsConstructor
public class VulTemplateController {

    private final VulTemplateService vulTemplateService;

    @GetMapping
    @Operation(summary = "分页查询模板")
    public ApiResponse<ListResult<VulTemplatePageVO.Response>> pageQuery(
            @ParameterObject VulTemplatePageVO.Request request) {
        return ApiResponse.success(vulTemplateService.pageQuery(request));
    }

    @GetMapping("/{templateId}")
    @Operation(summary = "获取模板详情")
    public ApiResponse<VulTemplateVO.Response> getById(@PathVariable Long templateId) {
        return ApiResponse.success(vulTemplateService.getById(templateId));
    }

    @DeleteMapping("/{templateId}")
    @Operation(summary = "删除模板")
    public ApiResponse<Void> delete(@PathVariable Long templateId) {
        vulTemplateService.delete(templateId);
        return ApiResponse.success();
    }

    @PostMapping("/import")
    @Operation(summary = "导入模板（JSON → 级联入库）")
    public ApiResponse<VulTemplateVO.Response> importTemplate(
            @RequestBody @Valid VulTemplateImportVO request) {
        return ApiResponse.success(
                vulTemplateService.importTemplate(request.getTemplate(), request.getCategoryIds()));
    }
}
