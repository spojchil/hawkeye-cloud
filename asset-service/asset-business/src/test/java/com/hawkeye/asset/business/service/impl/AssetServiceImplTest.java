package com.hawkeye.asset.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.common.utils.response.ListResult;
import com.hawkeye.asset.business.mapper.AssetCategoryMappingMapper;
import com.hawkeye.asset.business.mapper.AssetMapper;
import com.hawkeye.asset.business.mapstruct.AssetMapstruct;
import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import com.hawkeye.asset.common.pojo.entity.Asset;
import com.hawkeye.asset.common.pojo.entity.AssetCategoryMapping;
import com.hawkeye.asset.common.pojo.vo.asset.AssetVO;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssetServiceImpl 资产服务单元测试")
class AssetServiceImplTest {

    @Mock
    private AssetMapper assetMapper;
    @Mock
    private AssetCategoryMappingMapper assetCategoryMappingMapper;
    @Mock
    private AssetMapstruct assetMapstruct;

    private AssetServiceImpl assetService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, Asset.class);
        TableInfoHelper.initTableInfo(assistant, AssetCategoryMapping.class);
    }

    @BeforeEach
    void setUp() {
        assetService = new AssetServiceImpl(assetMapstruct, assetCategoryMappingMapper);
        ReflectionTestUtils.setField(assetService, "baseMapper", assetMapper);

        when(assetMapstruct.toEntity(any(AssetVO.Request.class))).thenAnswer(inv -> {
            AssetVO.Request req = inv.getArgument(0);
            Asset asset = new Asset();
            asset.setName(req.getName());
            asset.setRequestProtocol(req.getRequestProtocol());
            asset.setRequestHost(req.getRequestHost());
            asset.setRequestPort(req.getRequestPort());
            asset.setRequestPath(req.getRequestPath());
            asset.setDescription(req.getDescription());
            asset.setStatus(req.getStatus());
            asset.setRiskLevel(req.getRiskLevel());
            return asset;
        });

        when(assetMapstruct.toResponseVO(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            AssetVO.Response resp = new AssetVO.Response();
            resp.setAssetId(a.getAssetId());
            resp.setName(a.getName());
            resp.setRequestProtocol(a.getRequestProtocol());
            resp.setRequestHost(a.getRequestHost());
            resp.setRequestPort(a.getRequestPort());
            resp.setRequestPath(a.getRequestPath());
            resp.setDescription(a.getDescription());
            resp.setStatus(a.getStatus());
            resp.setRiskLevel(a.getRiskLevel());
            resp.setLastScanTime(a.getLastScanTime());
            return resp;
        });

        when(assetMapstruct.toListAssetVO(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            PageAssetVO.Response resp = new PageAssetVO.Response();
            resp.setAssetId(a.getAssetId());
            resp.setName(a.getName());
            resp.setRequestHost(a.getRequestHost());
            resp.setRiskLevel(a.getRiskLevel());
            resp.setStatus(a.getStatus());
            return resp;
        });
    }

    // === 分页查询 pageQuery ===

    @Test
    @DisplayName("基本分页查询 — 无任何筛选条件，使用默认分页参数（page=1, pageSize=10）")
    void pageQueryDefaults() {
        PageAssetVO.Request request = new PageAssetVO.Request();
        List<Asset> assets = List.of(
                buildAsset(1L, "api.example.com", "192.168.1.1", AssetRiskEnum.LOW, AssetStatusEnum.ENABLED),
                buildAsset(2L, "web.example.com", "192.168.1.2", AssetRiskEnum.MEDIUM, AssetStatusEnum.ENABLED)
        );
        stubSelectPage(assets);

        ListResult<PageAssetVO.Response> result = assetService.pageQuery(request);

        assertAll("默认分页查询",
                () -> assertEquals(2, result.getTotal()),
                () -> assertEquals(2, result.getData().size()),
                () -> assertEquals("api.example.com", result.getData().getFirst().getName()),
                () -> assertEquals(AssetRiskEnum.LOW, result.getData().getFirst().getRiskLevel())
        );
    }

    @Test
    @DisplayName("分页查询 — 按名称模糊筛选")
    void pageQueryWithNameFilter() {
        PageAssetVO.Request request = new PageAssetVO.Request();
        request.setName("api");
        Asset asset = buildAsset(1L, "api.example.com", "192.168.1.1", AssetRiskEnum.LOW, AssetStatusEnum.ENABLED);
        stubSelectPage(List.of(asset));

        ListResult<PageAssetVO.Response> result = assetService.pageQuery(request);

        assertAll("按名称筛选",
                () -> assertEquals(1, result.getTotal()),
                () -> assertEquals("api.example.com", result.getData().getFirst().getName())
        );
    }

    @Test
    @DisplayName("分页查询 — 按主机模糊筛选")
    void pageQueryWithHostFilter() {
        PageAssetVO.Request request = new PageAssetVO.Request();
        request.setRequestHost("192.168");
        Asset asset = buildAsset(1L, "db.example.com", "192.168.1.100", AssetRiskEnum.HIGH, AssetStatusEnum.ENABLED);
        stubSelectPage(List.of(asset));

        ListResult<PageAssetVO.Response> result = assetService.pageQuery(request);

        assertEquals(1, result.getTotal());
        assertEquals("192.168.1.100", result.getData().getFirst().getRequestHost());
    }

    @Test
    @DisplayName("分页查询 — 按风险等级筛选")
    void pageQueryWithRiskLevelFilter() {
        PageAssetVO.Request request = new PageAssetVO.Request();
        request.setRiskLevel(AssetRiskEnum.HIGH);
        Asset asset = buildAsset(1L, "danger.example.com", "10.0.0.1", AssetRiskEnum.HIGH, AssetStatusEnum.ENABLED);
        stubSelectPage(List.of(asset));

        ListResult<PageAssetVO.Response> result = assetService.pageQuery(request);

        assertEquals(1, result.getTotal());
        assertEquals(AssetRiskEnum.HIGH, result.getData().getFirst().getRiskLevel());
    }

    @Test
    @DisplayName("分页查询 — 按状态筛选")
    void pageQueryWithStatusFilter() {
        PageAssetVO.Request request = new PageAssetVO.Request();
        request.setStatus(AssetStatusEnum.DISABLED);
        Asset asset = buildAsset(1L, "old.example.com", "10.0.0.2", AssetRiskEnum.UNKNOWN, AssetStatusEnum.DISABLED);
        stubSelectPage(List.of(asset));

        ListResult<PageAssetVO.Response> result = assetService.pageQuery(request);

        assertEquals(1, result.getTotal());
        assertEquals(AssetStatusEnum.DISABLED, result.getData().getFirst().getStatus());
    }

    @Test
    @DisplayName("分页查询 — 按分类筛选，分类下有资产")
    void pageQueryWithCategoryFilter() {
        PageAssetVO.Request request = new PageAssetVO.Request();
        request.setCategoryId(5L);

        AssetCategoryMapping m1 = new AssetCategoryMapping();
        m1.setAssetId(1L);
        m1.setCategoryId(5L);
        AssetCategoryMapping m2 = new AssetCategoryMapping();
        m2.setAssetId(2L);
        m2.setCategoryId(5L);
        when(assetCategoryMappingMapper.selectList(any())).thenReturn(List.of(m1, m2));

        List<Asset> assets = List.of(
                buildAsset(1L, "a.com", "1.1.1.1", AssetRiskEnum.LOW, AssetStatusEnum.ENABLED),
                buildAsset(2L, "b.com", "2.2.2.2", AssetRiskEnum.MEDIUM, AssetStatusEnum.ENABLED)
        );
        stubSelectPage(assets);

        ListResult<PageAssetVO.Response> result = assetService.pageQuery(request);

        assertEquals(2, result.getTotal());
    }

    @Test
    @DisplayName("分页查询 — 按分类筛选，分类下没有资产，返回空列表，不执行分页查询")
    void pageQueryWithCategoryFilterEmpty() {
        PageAssetVO.Request request = new PageAssetVO.Request();
        request.setCategoryId(999L);
        when(assetCategoryMappingMapper.selectList(any())).thenReturn(Collections.emptyList());

        ListResult<PageAssetVO.Response> result = assetService.pageQuery(request);

        assertAll("分类下无资产",
                () -> assertEquals(0, result.getTotal()),
                () -> assertTrue(result.getData().isEmpty())
        );
        verify(assetMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("分页查询 — pageSize 超过最大限制 100 时自动截断为 100")
    void pageQuerySizeCapped() {
        PageAssetVO.Request request = new PageAssetVO.Request();
        request.setPageSize(9999);

        Asset asset = buildAsset(1L, "x.com", "1.1.1.1", AssetRiskEnum.LOW, AssetStatusEnum.ENABLED);
        when(assetMapper.selectPage(any(), any())).thenAnswer(inv -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Asset> page = inv.getArgument(0);
            assertEquals(100, page.getSize(), "pageSize 应被截断为 100");
            page.setRecords(List.of(asset));
            page.setTotal(1);
            return page;
        });

        assetService.pageQuery(request);
    }

    @Test
    @DisplayName("分页查询 — page 为 null 时默认第 1 页")
    void pageQueryPageNullDefaultsToOne() {
        PageAssetVO.Request request = new PageAssetVO.Request();
        request.setPage(null);
        request.setPageSize(10);

        when(assetMapper.selectPage(any(), any())).thenAnswer(inv -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Asset> page = inv.getArgument(0);
            assertEquals(1L, page.getCurrent(), "page 为 null 时应默认第 1 页");
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        assetService.pageQuery(request);
    }

    @Test
    @DisplayName("分页查询 — pageSize 为 null 时默认每页 10 条")
    void pageQuerySizeNullDefaultsToTen() {
        PageAssetVO.Request request = new PageAssetVO.Request();
        request.setPageSize(null);

        when(assetMapper.selectPage(any(), any())).thenAnswer(inv -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Asset> page = inv.getArgument(0);
            assertEquals(10, page.getSize(), "pageSize 为 null 时应默认 10");
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        assetService.pageQuery(request);
    }

    @Test
    @DisplayName("分页查询 — 分类过滤去重：同一 assetId 在 mapping 中出现多次只取一次")
    void pageQueryCategoryFilterDeduplicatesAssetIds() {
        PageAssetVO.Request request = new PageAssetVO.Request();
        request.setCategoryId(1L);

        AssetCategoryMapping m1 = new AssetCategoryMapping();
        m1.setAssetId(1L);
        AssetCategoryMapping m2 = new AssetCategoryMapping();
        m2.setAssetId(1L);
        when(assetCategoryMappingMapper.selectList(any())).thenReturn(List.of(m1, m2));

        Asset asset = buildAsset(1L, "dup.com", "1.1.1.1", AssetRiskEnum.LOW, AssetStatusEnum.ENABLED);
        stubSelectPage(List.of(asset));

        ListResult<PageAssetVO.Response> result = assetService.pageQuery(request);

        assertAll("去重后应只有一条",
                () -> assertEquals(1, result.getTotal()),
                () -> assertEquals(1, result.getData().size())
        );
    }

    // === 根据 ID 查询 getById ===

    @Test
    @DisplayName("根据 ID 查询 — 资产存在，返回完整详情 VO")
    void getByIdSuccess() {
        Asset asset = buildAsset(1L, "target.com", "10.0.0.1", AssetRiskEnum.HIGH, AssetStatusEnum.ENABLED);
        asset.setRequestProtocol("https");
        asset.setRequestPort(443);
        asset.setRequestPath("/api");
        asset.setDescription("目标资产");
        when(assetMapper.selectOne(any())).thenReturn(asset);

        AssetVO.Response response = assetService.getById(1L);

        assertAll("查询成功",
                () -> assertEquals(1L, response.getAssetId()),
                () -> assertEquals("target.com", response.getName()),
                () -> assertEquals("10.0.0.1", response.getRequestHost()),
                () -> assertEquals(AssetRiskEnum.HIGH, response.getRiskLevel()),
                () -> assertEquals("https", response.getRequestProtocol()),
                () -> assertEquals(443, response.getRequestPort()),
                () -> assertEquals("/api", response.getRequestPath()),
                () -> assertEquals("目标资产", response.getDescription())
        );
    }

    @Test
    @DisplayName("根据 ID 查询 — 资产不存在，抛出 ApiException（404）")
    void getByIdNotFound() {
        when(assetMapper.selectOne(any())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> assetService.getById(999L));

        assertAll("资产不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("资产不存在", ex.getMessage())
        );
    }

    // === 创建资产 create ===

    @Test
    @DisplayName("创建资产 — 未指定 status 和 riskLevel 时，自动设置为启用 + 未知")
    void createWithDefaults() {
        AssetVO.Request request = new AssetVO.Request();
        request.setName("new.example.com");
        request.setRequestProtocol("https");
        request.setRequestHost("10.0.0.50");
        request.setRequestPort(8443);
        request.setStatus(null);
        request.setRiskLevel(null);

        when(assetMapper.insert((Asset) any())).thenReturn(1);

        AssetVO.Response response = assetService.create(request);

        assertAll("创建成功，使用默认值",
                () -> assertEquals("new.example.com", response.getName()),
                () -> assertEquals(AssetStatusEnum.ENABLED, response.getStatus(),
                        "status 为 null 时应默认启用"),
                () -> assertEquals(AssetRiskEnum.UNKNOWN, response.getRiskLevel(),
                        "riskLevel 为 null 时应默认未知")
        );
        verify(assetMapper).insert((Asset) any());
    }

    @Test
    @DisplayName("创建资产 — 明确指定所有字段")
    void createWithExplicitValues() {
        AssetVO.Request request = new AssetVO.Request();
        request.setName("explicit.example.com");
        request.setRequestProtocol("http");
        request.setRequestHost("10.0.0.60");
        request.setRequestPort(8080);
        request.setRequestPath("/upload");
        request.setDescription("测试资产");
        request.setStatus(AssetStatusEnum.DISABLED);
        request.setRiskLevel(AssetRiskEnum.HIGH);

        when(assetMapper.insert((Asset) any())).thenReturn(1);

        AssetVO.Response response = assetService.create(request);

        assertAll("创建成功，保留指定值",
                () -> assertEquals("explicit.example.com", response.getName()),
                () -> assertEquals("http", response.getRequestProtocol()),
                () -> assertEquals("10.0.0.60", response.getRequestHost()),
                () -> assertEquals(8080, response.getRequestPort()),
                () -> assertEquals("/upload", response.getRequestPath()),
                () -> assertEquals("测试资产", response.getDescription()),
                () -> assertEquals(AssetStatusEnum.DISABLED, response.getStatus()),
                () -> assertEquals(AssetRiskEnum.HIGH, response.getRiskLevel())
        );
    }

    // === 更新资产 update ===

    @Test
    @DisplayName("更新资产 — 部分字段更新成功，null 字段不覆盖原值")
    void updateSuccess() {
        AssetVO.Request request = new AssetVO.Request();
        request.setName("updated-name");

        when(assetMapper.update(isNull(), any())).thenReturn(1);

        Asset updatedAsset = buildAsset(1L, "updated-name", "10.0.0.1", AssetRiskEnum.LOW, AssetStatusEnum.ENABLED);
        when(assetMapper.selectOne(any())).thenReturn(updatedAsset);

        AssetVO.Response response = assetService.update(1L, request);

        assertAll("部分更新成功",
                () -> assertEquals("updated-name", response.getName()),
                () -> assertEquals(1L, response.getAssetId())
        );
        verify(assetMapper).update(isNull(), any());
        verify(assetMapper).selectOne(any());
    }

    @Test
    @DisplayName("更新资产 — 资产不存在，update 影响 0 行，抛 ApiException（404）")
    void updateNotFound() {
        AssetVO.Request request = new AssetVO.Request();
        request.setName("ghost");

        when(assetMapper.update(isNull(), any())).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class,
                () -> assetService.update(999L, request));

        assertAll("更新不存在的资产",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("资产不存在", ex.getMessage())
        );
        verify(assetMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("更新资产 — 所有字段均为 null，抛 ApiException（400），不执行 SQL")
    void updateAllFieldsNull() {
        AssetVO.Request request = new AssetVO.Request();

        ApiException ex = assertThrows(ApiException.class,
                () -> assetService.update(1L, request));

        assertAll("全 null 更新请求",
                () -> assertEquals(CommonErrorCode.PARAM_INVALID.getCode(), ex.getCode()),
                () -> assertEquals("至少需要提供一个更新字段", ex.getMessage())
        );
        verify(assetMapper, never()).update(any(), any());
        verify(assetMapper, never()).selectOne(any());
    }

    // === 删除资产 delete ===

    @Test
    @DisplayName("删除资产 — 无分类关联，软删除成功，设置 deletedAt 时间戳")
    void deleteSuccess() {
        Asset asset = buildAsset(1L, "to-delete.com", "10.0.0.1", AssetRiskEnum.LOW, AssetStatusEnum.DISABLED);
        when(assetMapper.selectOne(any())).thenReturn(asset);
        when(assetCategoryMappingMapper.selectCount(any())).thenReturn(0L);
        when(assetMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        assetService.delete(1L);

        verify(assetMapper).selectOne(any());
        verify(assetCategoryMappingMapper).selectCount(any());
        verify(assetMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(assetMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除资产 — 资产不存在，抛 ApiException（404），不检查关联不执行删除")
    void deleteNotFound() {
        when(assetMapper.selectOne(any())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> assetService.delete(999L));

        assertAll("资产不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("资产不存在", ex.getMessage())
        );
        verify(assetMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(assetCategoryMappingMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("删除资产 — 存在分类关联，抛 ApiException（403），不执行删除")
    void deleteWithMappings() {
        Asset asset = buildAsset(1L, "has-mapping.com", "10.0.0.1", AssetRiskEnum.LOW, AssetStatusEnum.ENABLED);
        when(assetMapper.selectOne(any())).thenReturn(asset);
        when(assetCategoryMappingMapper.selectCount(any())).thenReturn(3L);

        ApiException ex = assertThrows(ApiException.class,
                () -> assetService.delete(1L));

        assertAll("存在分类关联",
                () -> assertEquals(CommonErrorCode.OPERATION_DENIED.getCode(), ex.getCode()),
                () -> assertEquals("该资产存在分类关联，请先移除所有关联再删除", ex.getMessage())
        );
        verify(assetMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    // === helper ===

    private Asset buildAsset(Long id, String name, String host, AssetRiskEnum risk, AssetStatusEnum status) {
        Asset asset = new Asset();
        asset.setAssetId(id);
        asset.setName(name);
        asset.setRequestProtocol("https");
        asset.setRequestHost(host);
        asset.setRequestPort(443);
        asset.setRiskLevel(risk);
        asset.setStatus(status);
        return asset;
    }

    private void stubSelectPage(List<Asset> assets) {
        when(assetMapper.selectPage(any(), any())).thenAnswer(inv -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Asset> page = inv.getArgument(0);
            page.setRecords(assets);
            page.setTotal(assets.size());
            return page;
        });
    }
}
