package com.hawkeye.auth.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.auth.business.mapper.TenantMapper;
import com.hawkeye.auth.business.service.TenantService;
import com.hawkeye.auth.common.pojo.entity.Tenant;
import com.hawkeye.auth.common.pojo.vo.authcontroller.TenantVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantServiceImpl extends ServiceImpl<TenantMapper, Tenant> implements TenantService {

    @Override
    @LogExecutionTime("创建租户")
    @Transactional
    public TenantVO.Response create(TenantVO.Request request) {
        Tenant tenant = new Tenant();
        tenant.setName(request.getName());
        tenant.setContactEmail(request.getContactEmail());
        tenant.setStatus(request.getStatus() != null ? request.getStatus() : 0);
        tenant.setMaxAssets(request.getMaxAssets());
        tenant.setMaxUsers(request.getMaxUsers());
        tenant.setMaxTasks(request.getMaxTasks());

        save(tenant);

        return toResponse(tenant);
    }

    @Override
    @LogExecutionTime("更新租户")
    @Transactional
    public TenantVO.Response update(Long tenantId, TenantVO.Request request) {
        Tenant tenant = getTenantOrThrow(tenantId);

        if (request.getName() != null) {
            tenant.setName(request.getName());
        }
        if (request.getContactEmail() != null) {
            tenant.setContactEmail(request.getContactEmail());
        }
        if (request.getStatus() != null) {
            tenant.setStatus(request.getStatus());
        }
        if (request.getMaxAssets() != null) {
            tenant.setMaxAssets(request.getMaxAssets());
        }
        if (request.getMaxUsers() != null) {
            tenant.setMaxUsers(request.getMaxUsers());
        }
        if (request.getMaxTasks() != null) {
            tenant.setMaxTasks(request.getMaxTasks());
        }

        updateById(tenant);

        return toResponse(tenant);
    }

    @Override
    @LogExecutionTime("删除租户")
    @Transactional
    public void delete(Long tenantId) {
        Tenant tenant = getTenantOrThrow(tenantId);
        removeById(tenant.getTenantId());
    }

    @Override
    @LogExecutionTime("查询租户详情")
    public TenantVO.Response getById(Long tenantId) {
        return toResponse(getTenantOrThrow(tenantId));
    }

    @Override
    @LogExecutionTime("查询租户列表")
    public List<TenantVO.Response> listAll() {
        return lambdaQuery()
                .eq(Tenant::getDeletedAt, 0L)
                .orderByAsc(Tenant::getTenantId)
                .list()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Tenant getTenantOrThrow(Long tenantId) {
        Tenant tenant = baseMapper.selectById(tenantId);
        if (tenant == null || tenant.getDeletedAt() != 0L) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "租户不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        return tenant;
    }

    private TenantVO.Response toResponse(Tenant tenant) {
        TenantVO.Response response = new TenantVO.Response();
        response.setTenantId(tenant.getTenantId());
        response.setName(tenant.getName());
        response.setContactEmail(tenant.getContactEmail());
        response.setStatus(tenant.getStatus());
        response.setMaxAssets(tenant.getMaxAssets());
        response.setMaxUsers(tenant.getMaxUsers());
        response.setMaxTasks(tenant.getMaxTasks());
        return response;
    }
}
