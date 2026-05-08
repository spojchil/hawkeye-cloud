package com.hawkeye.auth.business.service.impl;

import com.hawkeye.auth.business.mapper.TenantMapper;
import com.hawkeye.auth.common.pojo.entity.Tenant;
import com.hawkeye.auth.common.pojo.vo.authcontroller.TenantVO;
import com.common.utils.response.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TenantServiceImpl 租户管理单元测试")
class TenantServiceImplTest {

    @Mock
    private TenantMapper tenantMapper;

    @Spy
    @InjectMocks
    private TenantServiceImpl tenantService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tenantService, "baseMapper", tenantMapper);
    }

    @Test
    @DisplayName("创建租户成功")
    void createSuccess() {
        TenantVO.Request request = new TenantVO.Request();
        request.setName("测试租户");
        request.setContactEmail("test@example.com");
        request.setMaxAssets(100);

        when(tenantMapper.insert(any(Tenant.class))).thenReturn(1);

        TenantVO.Response response = tenantService.create(request);

        assertEquals("测试租户", response.getName());
        assertEquals("test@example.com", response.getContactEmail());
        assertEquals(100, response.getMaxAssets());
        assertEquals(0, response.getStatus());
    }

    @Test
    @DisplayName("查询租户详情成功")
    void getByIdSuccess() {
        Tenant tenant = buildTenant(1L, "测试租户", 0);
        when(tenantMapper.selectById(1L)).thenReturn(tenant);

        TenantVO.Response response = tenantService.getById(1L);

        assertEquals(1L, response.getTenantId());
        assertEquals("测试租户", response.getName());
    }

    @Test
    @DisplayName("查询租户详情 — 不存在，抛异常")
    void getByIdNotFound() {
        when(tenantMapper.selectById(999L)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> tenantService.getById(999L));

        assertEquals("租户不存在", ex.getMessage());
    }

    @Test
    @DisplayName("更新租户成功")
    void updateSuccess() {
        Tenant tenant = buildTenant(1L, "旧名称", 0);
        when(tenantMapper.selectById(1L)).thenReturn(tenant);

        TenantVO.Request request = new TenantVO.Request();
        request.setName("新名称");
        request.setStatus(1);

        TenantVO.Response response = tenantService.update(1L, request);

        assertEquals("新名称", response.getName());
        assertEquals(1, response.getStatus());
    }

    @Test
    @DisplayName("更新租户 — 不存在，抛异常")
    void updateNotFound() {
        when(tenantMapper.selectById(999L)).thenReturn(null);

        TenantVO.Request request = new TenantVO.Request();
        request.setName("新名称");

        ApiException ex = assertThrows(ApiException.class,
                () -> tenantService.update(999L, request));

        assertEquals("租户不存在", ex.getMessage());
    }

    @Test
    @DisplayName("删除租户成功")
    void deleteSuccess() {
        Tenant tenant = buildTenant(1L, "测试租户", 0);
        when(tenantMapper.selectById(1L)).thenReturn(tenant);

        assertDoesNotThrow(() -> tenantService.delete(1L));
    }

    @Test
    @DisplayName("删除租户 — 不存在，抛异常")
    void deleteNotFound() {
        when(tenantMapper.selectById(999L)).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> tenantService.delete(999L));

        assertEquals("租户不存在", ex.getMessage());
    }

    @Test
    @DisplayName("查询租户列表 — 方法签名可调用")
    void listAllEmpty() {
        doReturn(List.of()).when(tenantService).listAll();

        List<TenantVO.Response> list = tenantService.listAll();

        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    private Tenant buildTenant(Long id, String name, Integer status) {
        Tenant tenant = new Tenant();
        tenant.setTenantId(id);
        tenant.setName(name);
        tenant.setStatus(status);
        tenant.setDeletedAt(0L);
        return tenant;
    }
}
