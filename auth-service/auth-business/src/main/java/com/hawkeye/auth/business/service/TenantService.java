package com.hawkeye.auth.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hawkeye.auth.common.pojo.entity.Tenant;
import com.hawkeye.auth.common.pojo.vo.authcontroller.TenantVO;

import java.util.List;

public interface TenantService extends IService<Tenant> {

    TenantVO.Response create(TenantVO.Request request);

    TenantVO.Response update(Long tenantId, TenantVO.Request request);

    void delete(Long tenantId);

    TenantVO.Response getById(Long tenantId);

    List<TenantVO.Response> listAll();
}
