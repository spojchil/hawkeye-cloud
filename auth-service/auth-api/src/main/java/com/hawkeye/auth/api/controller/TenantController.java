package com.hawkeye.auth.api.controller;

import com.common.utils.response.ApiResponse;
import com.hawkeye.auth.business.service.TenantService;
import com.hawkeye.auth.common.pojo.vo.authcontroller.TenantVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth/tenants")
@RequiredArgsConstructor
@Tag(name = "租户管理")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @Operation(summary = "创建租户")
    public ApiResponse<TenantVO.Response> create(@RequestBody @Valid TenantVO.Request request) {
        return ApiResponse.success(tenantService.create(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询租户详情")
    public ApiResponse<TenantVO.Response> getById(@PathVariable Long id) {
        return ApiResponse.success(tenantService.getById(id));
    }

    @GetMapping
    @Operation(summary = "查询租户列表")
    public ApiResponse<List<TenantVO.Response>> listAll() {
        return ApiResponse.success(tenantService.listAll());
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新租户")
    public ApiResponse<TenantVO.Response> update(@PathVariable Long id,
                                                  @RequestBody @Valid TenantVO.Request request) {
        return ApiResponse.success(tenantService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除租户")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tenantService.delete(id);
        return ApiResponse.success(null);
    }
}
