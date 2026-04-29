package com.hawkeye.vul.api.controller;

import com.common.utils.response.ApiResponse;
import com.common.utils.response.ListResult;
import com.hawkeye.vul.business.service.VulTemplateService;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import com.hawkeye.vul.common.pojo.vo.vul.PageVulVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

@Tag(name = "漏洞模板管理", description = "漏洞检测模板的 CRUD")
@RestController
@RequestMapping("/vul")
@RequiredArgsConstructor
public class VulTemplateController {

    private final VulTemplateService vulTemplateService;

    @Operation(summary = "分页查询模板")
    @GetMapping
    public ApiResponse<ListResult<PageVulVO.Response>> pageQuery(@ParameterObject @Valid PageVulVO.Request request) {
        return ApiResponse.success(vulTemplateService.pageQuery(request));
    }

    @Operation(summary = "获取模板详情")
    @GetMapping("/{id}")
    public ApiResponse<VulVO.Response> getById(@PathVariable Long id) {
        return ApiResponse.success(vulTemplateService.getById(id));
    }

    @Operation(summary = "创建模板")
    @PostMapping
    public ApiResponse<VulVO.Response> create(@Valid @RequestBody VulVO.Request request) {
        return ApiResponse.success(vulTemplateService.create(request));
    }

    @Operation(summary = "更新模板")
    @PutMapping("/{id}")
    public ApiResponse<VulVO.Response> update(@PathVariable Long id, @Valid @RequestBody VulVO.Request request) {
        return ApiResponse.success(vulTemplateService.update(id, request));
    }

    @Operation(summary = "删除模板")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        vulTemplateService.delete(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "给检测引擎查询模板（Feign 内部接口）")
    @GetMapping("/internal/{id}")
    public ApiResponse<VulTemplateDetectDTO> getForDetection(@PathVariable Long id) {
        return ApiResponse.success(vulTemplateService.getForDetection(id));
    }
}
