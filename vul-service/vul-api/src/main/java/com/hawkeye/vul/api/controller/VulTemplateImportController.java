package com.hawkeye.vul.api.controller;

import com.common.utils.response.ApiResponse;
import com.hawkeye.vul.business.service.VulTemplateImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模板导入管理接口（仅管理员使用）。
 * <p>
 * 从本地 ddocs/http/ 目录读取 Nuclei YAML 模板并批量入库。
 * 已存在的 templateId 自动跳过，支持增量导入。
 */
@Tag(name = "模板导入管理")
@RestController
@RequestMapping("/vul-import")
@RequiredArgsConstructor
public class VulTemplateImportController {

    private final VulTemplateImportService importService;

    @Operation(summary = "全量导入所有分类的模板")
    @PostMapping("/all")
    public ApiResponse<VulTemplateImportService.ImportResult> importAll() {
        return ApiResponse.success(importService.importAll());
    }

    @Operation(summary = "按分类目录导入（如 cves / misconfiguration）")
    @PostMapping("/{categoryDir}")
    public ApiResponse<VulTemplateImportService.ImportResult> importByCategory(
            @PathVariable String categoryDir) {
        return ApiResponse.success(importService.importByCategory(categoryDir));
    }
}
