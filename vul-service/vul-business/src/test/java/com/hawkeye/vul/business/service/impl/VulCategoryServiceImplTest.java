package com.hawkeye.vul.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.vul.business.mapstruct.VulMapstruct;
import com.hawkeye.vul.business.mapper.VulCategoryMapper;
import com.hawkeye.vul.business.mapper.VulCategoryMappingMapper;
import com.hawkeye.vul.common.pojo.entity.VulCategory;
import com.hawkeye.vul.common.pojo.entity.VulCategoryMapping;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryVO;
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
@DisplayName("VulCategoryServiceImpl 漏洞分类服务单元测试")
class VulCategoryServiceImplTest {

    @Mock
    private VulCategoryMapper categoryMapper;
    @Mock
    private VulCategoryMappingMapper mappingMapper;
    @Mock
    private VulMapstruct vulMapstruct;

    private VulCategoryServiceImpl categoryService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, VulCategory.class);
        TableInfoHelper.initTableInfo(assistant, VulCategoryMapping.class);
    }

    @BeforeEach
    void setUp() {
        categoryService = new VulCategoryServiceImpl(vulMapstruct, mappingMapper);
        ReflectionTestUtils.setField(categoryService, "baseMapper", categoryMapper);

        when(vulMapstruct.toCategoryEntity(any(VulCategoryVO.Request.class))).thenAnswer(inv -> {
            VulCategoryVO.Request req = inv.getArgument(0);
            VulCategory entity = new VulCategory();
            entity.setName(req.getName());
            entity.setParentId(req.getParentId());
            entity.setDescription(req.getDescription());
            return entity;
        });

        when(vulMapstruct.toCategoryVO(any(VulCategory.class))).thenAnswer(inv -> {
            VulCategory entity = inv.getArgument(0);
            VulCategoryVO.Response resp = new VulCategoryVO.Response();
            resp.setCategoryId(entity.getCategoryId());
            resp.setName(entity.getName());
            resp.setParentId(entity.getParentId());
            resp.setDescription(entity.getDescription());
            return resp;
        });
    }

    // === listCategories ===

    @Test
    @DisplayName("查询顶级分类 — parentId 为 null 时查 parentId IS NULL，不返回子分类")
    void listTopLevelCategories() {
        VulCategory c1 = buildCategory(1L, "Web 漏洞", null);
        VulCategory c2 = buildCategory(2L, "主机漏洞", null);
        when(categoryMapper.selectList(any())).thenReturn(List.of(c1, c2));

        List<VulCategoryVO.Response> result = categoryService.listCategories(null, null);

        assertAll("顶级分类列表",
                () -> assertEquals(2, result.size()),
                () -> assertEquals("Web 漏洞", result.get(0).getName()),
                () -> assertNull(result.get(0).getParentId())
        );
    }

    @Test
    @DisplayName("查询子分类 — 指定 parentId 返回该父分类下的所有子分类")
    void listSubCategories() {
        VulCategory c1 = buildCategory(10L, "XSS", 1L);
        VulCategory c2 = buildCategory(11L, "SQL 注入", 1L);
        when(categoryMapper.selectList(any())).thenReturn(List.of(c1, c2));

        List<VulCategoryVO.Response> result = categoryService.listCategories(1L, null);

        assertAll("子分类列表",
                () -> assertEquals(2, result.size()),
                () -> assertEquals(1L, result.get(0).getParentId()),
                () -> assertEquals(1L, result.get(1).getParentId())
        );
    }

    @Test
    @DisplayName("查询分类 — 按名称模糊筛选")
    void listWithNameFilter() {
        VulCategory c1 = buildCategory(1L, "Web 安全", null);
        when(categoryMapper.selectList(any())).thenReturn(List.of(c1));

        List<VulCategoryVO.Response> result = categoryService.listCategories(null, "Web");

        assertEquals(1, result.size());
        assertEquals("Web 安全", result.get(0).getName());
    }

    @Test
    @DisplayName("查询分类 — 空结果返回空列表")
    void listEmptyResult() {
        when(categoryMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<VulCategoryVO.Response> result = categoryService.listCategories(null, "不存在");

        assertTrue(result.isEmpty());
    }

    // === create ===

    @Test
    @DisplayName("创建顶级分类 — parentId 为 null")
    void createCategory() {
        VulCategoryVO.Request request = new VulCategoryVO.Request();
        request.setName("新分类");
        request.setDescription("描述");

        when(categoryMapper.insert((VulCategory) any())).thenReturn(1);

        VulCategoryVO.Response response = categoryService.create(request);

        assertAll("创建分类",
                () -> assertEquals("新分类", response.getName()),
                () -> assertEquals("描述", response.getDescription()),
                () -> assertNull(response.getParentId())
        );
        verify(categoryMapper).insert((VulCategory) any());
    }

    @Test
    @DisplayName("创建子分类 — 指定 parentId")
    void createCategoryWithParent() {
        VulCategoryVO.Request request = new VulCategoryVO.Request();
        request.setName("子分类");
        request.setParentId(5L);

        when(categoryMapper.insert((VulCategory) any())).thenReturn(1);

        VulCategoryVO.Response response = categoryService.create(request);

        assertEquals(5L, response.getParentId());
    }

    // === update ===

    @Test
    @DisplayName("更新分类 — 部分字段更新成功，防环通过")
    void updateCategorySuccess() {
        VulCategoryVO.Request request = new VulCategoryVO.Request();
        request.setName("新名称");

        VulCategory existing = buildCategory(1L, "原名称", null);
        when(categoryMapper.selectById(1L)).thenReturn(existing);
        when(categoryMapper.update(isNull(), any())).thenReturn(1);

        VulCategory updated = buildCategory(1L, "新名称", null);
        when(categoryMapper.selectById(1L)).thenReturn(existing).thenReturn(updated);

        VulCategoryVO.Response response = categoryService.update(1L, request);

        assertAll("更新成功",
                () -> assertEquals("新名称", response.getName()),
                () -> assertEquals(1L, response.getCategoryId())
        );
    }

    @Test
    @DisplayName("更新分类 — parentId 等于自身，抛 400")
    void updateParentIdSelf() {
        VulCategoryVO.Request request = new VulCategoryVO.Request();
        request.setParentId(1L);

        VulCategory existing = buildCategory(1L, "分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(existing);

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.update(1L, request));

        assertAll("父分类不能是自己",
                () -> assertEquals(CommonErrorCode.PARAM_INVALID.getCode(), ex.getCode()),
                () -> assertEquals("父分类不能是自己", ex.getMessage())
        );
        verify(categoryMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("更新分类 — 分类不存在，抛 404")
    void updateCategoryNotFound() {
        VulCategoryVO.Request request = new VulCategoryVO.Request();
        request.setName("不存在");

        when(categoryMapper.selectById(anyLong())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.update(999L, request));

        assertAll("分类不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("分类不存在", ex.getMessage())
        );
    }

    // === delete ===

    @Test
    @DisplayName("删除分类 — 无子分类且无模板关联，删除成功")
    void deleteCategorySuccess() {
        VulCategory category = buildCategory(1L, "可删除", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(mappingMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.deleteById(1L)).thenReturn(1);

        categoryService.delete(1L);

        verify(categoryMapper).selectById(1L);
        verify(categoryMapper).selectCount(any());
        verify(mappingMapper).selectCount(any());
        verify(categoryMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除分类 — 分类不存在，抛 404")
    void deleteCategoryNotFound() {
        when(categoryMapper.selectById(anyLong())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.delete(999L));

        assertAll("分类不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("分类不存在", ex.getMessage())
        );
        verify(categoryMapper, never()).deleteById(anyLong());
        verify(categoryMapper, never()).selectCount(any());
        verify(mappingMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("删除分类 — 存在子分类，抛 403")
    void deleteCategoryWithChildren() {
        VulCategory category = buildCategory(1L, "有子分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(categoryMapper.selectCount(any())).thenReturn(5L);

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.delete(1L));

        assertAll("存在子分类",
                () -> assertEquals(CommonErrorCode.OPERATION_DENIED.getCode(), ex.getCode()),
                () -> assertEquals("该分类下存在子分类，请先删除子分类", ex.getMessage())
        );
        verify(categoryMapper, never()).deleteById(anyLong());
        verify(mappingMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("删除分类 — 存在模板关联，抛 403")
    void deleteCategoryWithMappings() {
        VulCategory category = buildCategory(1L, "有模板关联", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(mappingMapper.selectCount(any())).thenReturn(3L);

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.delete(1L));

        assertAll("存在模板关联",
                () -> assertEquals(CommonErrorCode.OPERATION_DENIED.getCode(), ex.getCode()),
                () -> assertEquals("该分类下存在关联模板，请先移除所有模板", ex.getMessage())
        );
        verify(categoryMapper, never()).deleteById(anyLong());
    }

    // === addTemplates ===

    @Test
    @DisplayName("添加模板 — 全部是新关联，批量插入，返回新增数量")
    void addTemplatesAllNew() {
        VulCategory category = buildCategory(1L, "测试分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(mappingMapper.selectList(any())).thenReturn(Collections.emptyList());

        int count = categoryService.addTemplates(1L, List.of(10L, 20L, 30L));

        assertEquals(3, count, "应新增 3 条关联");
    }

    @Test
    @DisplayName("添加模板 — 部分已存在，去重后仅插入新关联")
    void addTemplatesWithExisting() {
        VulCategory category = buildCategory(1L, "测试分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);

        VulCategoryMapping existing1 = new VulCategoryMapping();
        existing1.setTemplateId(10L);
        VulCategoryMapping existing2 = new VulCategoryMapping();
        existing2.setTemplateId(20L);
        when(mappingMapper.selectList(any())).thenReturn(List.of(existing1, existing2));

        int count = categoryService.addTemplates(1L, List.of(10L, 20L, 30L, 40L));

        assertEquals(2, count, "跳过 2 个已存在 → 新增 2 个");
    }

    @Test
    @DisplayName("添加模板 — 重复 templateId 去重")
    void addTemplatesInputDuplicate() {
        VulCategory category = buildCategory(1L, "测试分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(mappingMapper.selectList(any())).thenReturn(Collections.emptyList());

        int count = categoryService.addTemplates(1L, List.of(10L, 10L));

        assertEquals(1, count, "输入重复应去重为 1 条");
    }

    @Test
    @DisplayName("添加模板 — 分类不存在，抛 404")
    void addTemplatesCategoryNotFound() {
        when(categoryMapper.selectById(anyLong())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> categoryService.addTemplates(999L, List.of(1L, 2L)));

        assertAll("分类不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("分类不存在", ex.getMessage())
        );
        verify(mappingMapper, never()).selectList(any());
        verify(mappingMapper, never()).insert(any(VulCategoryMapping.class));
    }

    @Test
    @DisplayName("添加模板 — 全部已存在，返回 0，不执行插入")
    void addTemplatesAllExisting() {
        VulCategory category = buildCategory(1L, "测试分类", null);
        when(categoryMapper.selectById(1L)).thenReturn(category);

        VulCategoryMapping existing1 = new VulCategoryMapping();
        existing1.setTemplateId(10L);
        VulCategoryMapping existing2 = new VulCategoryMapping();
        existing2.setTemplateId(20L);
        when(mappingMapper.selectList(any())).thenReturn(List.of(existing1, existing2));

        int count = categoryService.addTemplates(1L, List.of(10L, 20L));

        assertEquals(0, count, "全部已存在 → 0 条新增");
    }

    // === removeTemplates ===

    @Test
    @DisplayName("移除模板 — 按分类+ID列表删除，返回删除行数")
    void removeTemplates() {
        when(mappingMapper.delete(any())).thenReturn(3);

        int count = categoryService.removeTemplates(1L, List.of(10L, 20L, 30L));

        assertEquals(3, count);
        verify(mappingMapper).delete(any());
    }

    @Test
    @DisplayName("移除模板 — 空列表返回 0")
    void removeTemplatesEmptyList() {
        int count = categoryService.removeTemplates(1L, Collections.emptyList());

        assertEquals(0, count);
        verify(mappingMapper, never()).delete(any());
    }

    // === helper ===

    private VulCategory buildCategory(Long id, String name, Long parentId) {
        VulCategory category = new VulCategory();
        category.setCategoryId(id);
        category.setName(name);
        category.setParentId(parentId);
        return category;
    }
}
