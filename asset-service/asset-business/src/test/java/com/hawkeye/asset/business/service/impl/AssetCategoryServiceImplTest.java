package com.hawkeye.asset.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.asset.business.mapstruct.AssetCategoryMapstruct;
import com.hawkeye.asset.business.mapper.AssetCategoryMapper;
import com.hawkeye.asset.business.mapper.AssetCategoryMappingMapper;
import com.hawkeye.asset.common.pojo.entity.AssetCategory;
import com.hawkeye.asset.common.pojo.entity.AssetCategoryMapping;
import com.hawkeye.asset.common.pojo.vo.category.CategoryVO;
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
@DisplayName("AssetCategoryServiceImpl 分类服务单元测试")
class AssetCategoryServiceImplTest {

    @Mock
    private AssetCategoryMapper categoryMapper;
    @Mock
    private AssetCategoryMappingMapper mappingMapper;
    @Mock
    private AssetCategoryMapstruct categoryMapstruct;
    @Mock
    private LambdaQueryChainWrapper<AssetCategory> lambdaChain;

    private AssetCategoryServiceImpl categoryService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, AssetCategory.class);
        TableInfoHelper.initTableInfo(assistant, AssetCategoryMapping.class);
    }

    @BeforeEach
    void setUp() {
        /*
         * MyBatis-Plus 的 lambdaQuery() 会对 mapper 做代理元数据自省，
         * 无法用 Mockito 直接 mock 通过，所以 stub 掉整个 lambdaQuery() 链。
         * 必须用 spy 才能劫持 lambdaQuery() 方法的调用。
         */
        categoryService = spy(new AssetCategoryServiceImpl(categoryMapstruct, mappingMapper));
        ReflectionTestUtils.setField(categoryService, "baseMapper", categoryMapper);

        doReturn(lambdaChain).when(categoryService).lambdaQuery();
        when(lambdaChain.eq(any(), any())).thenReturn(lambdaChain);

        when(categoryMapstruct.toEntity(any(CategoryVO.Request.class))).thenAnswer(inv -> {
            CategoryVO.Request req = inv.getArgument(0);
            AssetCategory entity = new AssetCategory();
            entity.setName(req.getName());
            entity.setParentId(req.getParentId());
            entity.setDescription(req.getDescription());
            return entity;
        });

        when(categoryMapstruct.toResponseVO(any(AssetCategory.class))).thenAnswer(inv -> {
            AssetCategory entity = inv.getArgument(0);
            CategoryVO.Response resp = new CategoryVO.Response();
            resp.setCategoryId(entity.getCategoryId());
            resp.setName(entity.getName());
            resp.setParentId(entity.getParentId());
            resp.setDescription(entity.getDescription());
            return resp;
        });
    }

    // === 查询分类列表 listCategories ===

    @Test
    @DisplayName("查询顶级分类 — parentId 为 null 时只查 parentId IS NULL 的分类")
    void listTopLevelCategories() {
        AssetCategory c1 = buildCategory(1L, "Web 应用", null);
        AssetCategory c2 = buildCategory(2L, "API 服务", null);
        when(categoryMapper.selectList(any())).thenReturn(List.of(c1, c2));

        List<CategoryVO.Response> result = categoryService.listCategories(null, null);

        assertAll("顶级分类列表",
                () -> assertEquals(2, result.size()),
                () -> assertEquals("Web 应用", result.get(0).getName()),
                () -> assertEquals("API 服务", result.get(1).getName()),
                () -> assertNull(result.get(0).getParentId(), "顶级分类的 parentId 应为 null")
        );
    }

    @Test
    @DisplayName("查询子分类 — 指定 parentId 查某个父分类下的所有子分类")
    void listSubCategories() {
        AssetCategory c1 = buildCategory(10L, "前端", 1L);
        AssetCategory c2 = buildCategory(11L, "后端", 1L);
        when(categoryMapper.selectList(any())).thenReturn(List.of(c1, c2));

        List<CategoryVO.Response> result = categoryService.listCategories(1L, null);

        assertAll("子分类列表",
                () -> assertEquals(2, result.size()),
                () -> assertEquals(1L, result.get(0).getParentId()),
                () -> assertEquals(1L, result.get(1).getParentId())
        );
    }

    @Test
    @DisplayName("查询分类 — 按名称模糊筛选")
    void listWithNameFilter() {
        AssetCategory c1 = buildCategory(1L, "Web 安全", null);
        when(categoryMapper.selectList(any())).thenReturn(List.of(c1));

        List<CategoryVO.Response> result = categoryService.listCategories(null, "Web");

        assertEquals(1, result.size());
        assertEquals("Web 安全", result.get(0).getName());
    }

    @Test
    @DisplayName("查询分类 — 空结果返回空列表不抛异常")
    void listEmptyResult() {
        when(categoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<CategoryVO.Response> result = categoryService.listCategories(null, "不存在的分类");

        assertTrue(result.isEmpty());
    }

    // === 创建分类 create ===

    @Test
    @DisplayName("创建顶级分类 — parentId 为 null")
    void createCategory() {
        CategoryVO.Request request = new CategoryVO.Request();
        request.setName("新分类");
        request.setDescription("描述");

        when(categoryMapper.insert((AssetCategory) any())).thenReturn(1);

        CategoryVO.Response response = categoryService.create(request);

        assertAll("创建分类",
                () -> assertEquals("新分类", response.getName()),
                () -> assertEquals("描述", response.getDescription()),
                () -> assertNull(response.getParentId())
        );
        verify(categoryMapper).insert((AssetCategory) any());
    }

    @Test
    @DisplayName("创建子分类 — 指定 parentId")
    void createCategoryWithParent() {
        CategoryVO.Request request = new CategoryVO.Request();
        request.setName("子分类");
        request.setParentId(5L);

        when(categoryMapper.insert((AssetCategory) any())).thenReturn(1);

        CategoryVO.Response response = categoryService.create(request);

        assertEquals(5L, response.getParentId());
    }

    // === 更新分类 update ===

    @Test
    @DisplayName("更新分类 — 部分字段更新成功，忽略 parentId（不允许修改父分类）")
    void updateCategorySuccess() {
        CategoryVO.Request request = new CategoryVO.Request();
        request.setName("新名称");
        request.setDescription("新描述");
        request.setParentId(999L); // 不允许修改父分类，应被忽略

        when(categoryMapper.update(isNull(), any())).thenReturn(1);

        AssetCategory updated = buildCategory(1L, "新名称", 5L);
        updated.setDescription("新描述");
        when(categoryMapper.selectById(1L)).thenReturn(updated);

        CategoryVO.Response response = categoryService.update(1L, request);

        assertAll("更新成功",
                () -> assertEquals("新名称", response.getName()),
                () -> assertEquals("新描述", response.getDescription()),
                () -> assertEquals(5L, response.getParentId(),
                        "parentId 应保持原值，未被修改")
        );
        verify(categoryMapper).selectById(1L);
    }

    @Test
    @DisplayName("更新分类 — 分类不存在，update 影响 0 行，抛 ApiException（404）")
    void updateCategoryNotFound() {
        CategoryVO.Request request = new CategoryVO.Request();
        request.setName("不存在");

        when(categoryMapper.update(isNull(), any())).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.update(999L, request));

        assertAll("分类不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("分类不存在", ex.getMessage())
        );
    }

    @Test
    @DisplayName("更新分类 — 所有字段均为 null，抛 ApiException（400），不执行 SQL")
    void updateCategoryAllFieldsNull() {
        CategoryVO.Request request = new CategoryVO.Request();

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.update(1L, request));

        assertAll("全 null 更新请求",
                () -> assertEquals(CommonErrorCode.PARAM_INVALID.getCode(), ex.getCode()),
                () -> assertEquals("至少需要提供一个更新字段", ex.getMessage())
        );
        verify(categoryMapper, never()).update(any(), any());
        verify(categoryMapper, never()).selectById(anyLong());
    }

    // === 删除分类 delete ===

    @Test
    @DisplayName("删除分类 — 无子分类且无资产关联，删除成功")
    void deleteCategorySuccess() {
        AssetCategory category = buildCategory(1L, "可删除", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(lambdaChain.count()).thenReturn(0L);
        when(mappingMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.deleteById(1L)).thenReturn(1);

        categoryService.delete(1L);

        verify(categoryMapper).selectById(1L);
        verify(lambdaChain).count();
        verify(mappingMapper).selectCount(any());
        verify(categoryMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除分类 — 分类不存在，抛 ApiException（404），不执行后续检查")
    void deleteCategoryNotFound() {
        when(categoryMapper.selectById(anyLong())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.delete(999L));

        assertAll("分类不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("分类不存在", ex.getMessage())
        );
        verify(lambdaChain, never()).count();
        verify(mappingMapper, never()).selectCount(any());
        verify(categoryMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除分类 — 存在子分类，抛 ApiException（403），不执行删除也不检查资产关联")
    void deleteCategoryWithChildren() {
        AssetCategory category = buildCategory(1L, "有子分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(lambdaChain.count()).thenReturn(5L);

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.delete(1L));

        assertAll("存在子分类",
                () -> assertEquals(CommonErrorCode.OPERATION_DENIED.getCode(), ex.getCode()),
                () -> assertEquals("该分类下存在子分类，无法删除", ex.getMessage())
        );
        verify(categoryMapper, never()).deleteById(anyLong());
        verify(mappingMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("删除分类 — 存在资产关联，抛 ApiException（403），不执行删除")
    void deleteCategoryWithMappings() {
        AssetCategory category = buildCategory(1L, "有资产关联", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(lambdaChain.count()).thenReturn(0L);
        when(mappingMapper.selectCount(any())).thenReturn(3L);

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.delete(1L));

        assertAll("存在资产关联",
                () -> assertEquals(CommonErrorCode.OPERATION_DENIED.getCode(), ex.getCode()),
                () -> assertEquals("该分类下存在资产关联，无法删除", ex.getMessage())
        );
        verify(categoryMapper, never()).deleteById(anyLong());
    }

    // === 添加资产到分类 addAssets ===

    @Test
    @DisplayName("添加资产 — 全部是新关联，逐个插入，返回新增数量")
    void addAssetsAllNew() {
        AssetCategory category = buildCategory(1L, "测试分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(mappingMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(mappingMapper.insert(any(AssetCategoryMapping.class))).thenReturn(1);

        int count = categoryService.addAssets(1L, List.of(10L, 20L, 30L));

        assertEquals(3, count, "应新增 3 条关联");
        verify(mappingMapper, times(3)).insert(any(AssetCategoryMapping.class));
    }

    @Test
    @DisplayName("添加资产 — 部分已存在，去重后仅插入新的，返回新增数量")
    void addAssetsWithExisting() {
        AssetCategory category = buildCategory(1L, "测试分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);

        AssetCategoryMapping existing1 = new AssetCategoryMapping();
        existing1.setAssetId(10L);
        AssetCategoryMapping existing2 = new AssetCategoryMapping();
        existing2.setAssetId(20L);
        when(mappingMapper.selectList(any())).thenReturn(List.of(existing1, existing2));
        when(mappingMapper.insert(any(AssetCategoryMapping.class))).thenReturn(1);

        int count = categoryService.addAssets(1L, List.of(10L, 20L, 30L, 40L));

        assertEquals(2, count, "应新增 2 条关联（跳过已存在的 2 个）");
        verify(mappingMapper, times(2)).insert(any(AssetCategoryMapping.class));
    }

    @Test
    @DisplayName("添加资产 — 重复 assetId 去重：传入 [10, 10] 只插入一次")
    void addAssetsInputDuplicate() {
        AssetCategory category = buildCategory(1L, "测试分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(mappingMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(mappingMapper.insert(any(AssetCategoryMapping.class))).thenReturn(1);

        int count = categoryService.addAssets(1L, List.of(10L, 10L));

        assertEquals(1, count, "重复的 assetId 应去重为 1 条");
        verify(mappingMapper, times(1)).insert(any(AssetCategoryMapping.class));
    }

    @Test
    @DisplayName("添加资产 — 分类不存在，抛 ApiException（404），不执行插入")
    void addAssetsCategoryNotFound() {
        when(categoryMapper.selectById(anyLong())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.addAssets(999L, List.of(1L, 2L)));

        assertAll("分类不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("分类不存在", ex.getMessage())
        );
        verify(mappingMapper, never()).selectList(any());
        verify(mappingMapper, never()).insert(any(AssetCategoryMapping.class));
    }

    @Test
    @DisplayName("添加资产 — 传入空列表，返回 0，不执行插入")
    void addAssetsEmptyList() {
        AssetCategory category = buildCategory(1L, "测试分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(mappingMapper.selectList(any())).thenReturn(Collections.emptyList());

        int count = categoryService.addAssets(1L, Collections.emptyList());

        assertEquals(0, count);
        verify(mappingMapper, never()).insert(any(AssetCategoryMapping.class));
    }

    @Test
    @DisplayName("添加资产 — 所有 assetId 都已存在，跳过全部，返回 0")
    void addAssetsAllExisting() {
        AssetCategory category = buildCategory(1L, "测试分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);

        AssetCategoryMapping existing1 = new AssetCategoryMapping();
        existing1.setAssetId(10L);
        AssetCategoryMapping existing2 = new AssetCategoryMapping();
        existing2.setAssetId(20L);
        when(mappingMapper.selectList(any())).thenReturn(List.of(existing1, existing2));

        int count = categoryService.addAssets(1L, List.of(10L, 20L));

        assertEquals(0, count, "全部已存在时应返回 0");
        verify(mappingMapper, never()).insert(any(AssetCategoryMapping.class));
    }

    // === 移除资产关联 removeAssets ===

    @Test
    @DisplayName("移除资产关联 — 按分类+资产ID列表删除，返回删除行数")
    void removeAssets() {
        when(mappingMapper.delete(any())).thenReturn(3);

        int count = categoryService.removeAssets(1L, List.of(10L, 20L, 30L));

        assertEquals(3, count);
        verify(mappingMapper).delete(any());
    }

    @Test
    @DisplayName("移除资产关联 — 空列表，返回 0")
    void removeAssetsEmptyList() {
        when(mappingMapper.delete(any())).thenReturn(0);

        int count = categoryService.removeAssets(1L, Collections.emptyList());

        assertEquals(0, count);
    }

    // === helper ===

    private AssetCategory buildCategory(Long id, String name, Long parentId) {
        AssetCategory category = new AssetCategory();
        category.setCategoryId(id);
        category.setName(name);
        category.setParentId(parentId);
        return category;
    }
}
