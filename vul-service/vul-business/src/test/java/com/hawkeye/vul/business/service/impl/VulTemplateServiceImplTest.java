package com.hawkeye.vul.business.service.impl;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.common.utils.response.ListResult;
import com.hawkeye.vul.business.mapstruct.VulMapstruct;
import com.hawkeye.vul.business.mapper.VulCategoryMappingMapper;
import com.hawkeye.vul.business.mapper.VulTemplateMapper;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import com.hawkeye.vul.common.pojo.entity.VulCategoryMapping;
import com.hawkeye.vul.common.pojo.entity.VulTemplate;
import com.hawkeye.vul.common.pojo.vo.vul.PageVulVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulVO;
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
@DisplayName("VulTemplateServiceImpl 漏洞模板服务单元测试")
class VulTemplateServiceImplTest {

    @Mock
    private VulTemplateMapper templateMapper;
    @Mock
    private VulCategoryMappingMapper mappingMapper;
    @Mock
    private VulMapstruct vulMapstruct;

    private VulTemplateServiceImpl templateService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        Configuration configuration = new Configuration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, VulTemplate.class);
        TableInfoHelper.initTableInfo(assistant, VulCategoryMapping.class);
    }

    @BeforeEach
    void setUp() {
        templateService = new VulTemplateServiceImpl(vulMapstruct, mappingMapper);
        ReflectionTestUtils.setField(templateService, "baseMapper", templateMapper);

        when(vulMapstruct.toEntity(any(VulVO.Request.class))).thenAnswer(inv -> {
            VulVO.Request req = inv.getArgument(0);
            VulTemplate t = new VulTemplate();
            t.setTemplateId(req.getTemplateId());
            t.setName(req.getName());
            t.setDescription(req.getDescription());
            t.setAuthor(req.getAuthor());
            t.setSeverity(req.getSeverity());
            t.setTags(req.getTags());
            t.setReference(req.getReference());
            t.setClassification(req.getClassification());
            t.setMetadata(req.getMetadata());
            t.setFlow(req.getFlow());
            t.setVariables(req.getVariables());
            t.setHttpRequests(req.getHttpRequests());
            t.setMatchers(req.getMatchers());
            t.setExtractors(req.getExtractors());
            t.setEnabled(req.getEnabled());
            t.setVersion(req.getVersion());
            return t;
        });

        when(vulMapstruct.toResponseVO(any(VulTemplate.class))).thenAnswer(inv -> {
            VulTemplate t = inv.getArgument(0);
            VulVO.Response resp = new VulVO.Response();
            resp.setId(t.getId());
            resp.setTemplateId(t.getTemplateId());
            resp.setName(t.getName());
            resp.setDescription(t.getDescription());
            resp.setAuthor(t.getAuthor());
            resp.setSeverity(t.getSeverity());
            resp.setTags(t.getTags());
            resp.setReference(t.getReference());
            resp.setClassification(t.getClassification());
            resp.setMetadata(t.getMetadata());
            resp.setFlow(t.getFlow());
            resp.setVariables(t.getVariables());
            resp.setHttpRequests(t.getHttpRequests());
            resp.setMatchers(t.getMatchers());
            resp.setExtractors(t.getExtractors());
            resp.setEnabled(t.getEnabled());
            resp.setVersion(t.getVersion());
            return resp;
        });

        when(vulMapstruct.toPageVO(any(VulTemplate.class))).thenAnswer(inv -> {
            VulTemplate t = inv.getArgument(0);
            PageVulVO.Response resp = new PageVulVO.Response();
            resp.setId(t.getId());
            resp.setTemplateId(t.getTemplateId());
            resp.setName(t.getName());
            resp.setSeverity(t.getSeverity());
            resp.setTags(t.getTags());
            resp.setEnabled(t.getEnabled());
            resp.setVersion(t.getVersion());
            return resp;
        });
    }

    // === pageQuery ===

    @Test
    @DisplayName("基本分页查询 — 无筛选条件")
    void pageQueryDefaults() {
        PageVulVO.Request request = new PageVulVO.Request();
        List<VulTemplate> templates = List.of(
                buildTemplate(1L, "CVE-2024-0012", "XSS 漏洞", "low", true, 1),
                buildTemplate(2L, "CVE-2024-0013", "SQL 注入", "high", true, 1)
        );
        stubSelectPage(templates);

        ListResult<PageVulVO.Response> result = templateService.pageQuery(request);

        assertAll("默认分页",
                () -> assertEquals(2, result.getTotal()),
                () -> assertEquals(2, result.getData().size()),
                () -> assertEquals("CVE-2024-0012", result.getData().get(0).getTemplateId()),
                () -> assertEquals("low", result.getData().get(0).getSeverity())
        );
    }

    @Test
    @DisplayName("分页查询 — 按名称模糊筛选")
    void pageQueryWithNameFilter() {
        PageVulVO.Request request = new PageVulVO.Request();
        request.setName("XSS");
        VulTemplate t = buildTemplate(1L, "CVE-001", "XSS 漏洞", "medium", true, 1);
        stubSelectPage(List.of(t));

        ListResult<PageVulVO.Response> result = templateService.pageQuery(request);

        assertEquals(1, result.getTotal());
        assertEquals("XSS 漏洞", result.getData().get(0).getName());
    }

    @Test
    @DisplayName("分页查询 — 按严重程度筛选")
    void pageQueryWithSeverityFilter() {
        PageVulVO.Request request = new PageVulVO.Request();
        request.setSeverity("critical");
        VulTemplate t = buildTemplate(1L, "CVE-001", "严重漏洞", "critical", true, 1);
        stubSelectPage(List.of(t));

        ListResult<PageVulVO.Response> result = templateService.pageQuery(request);

        assertEquals(1, result.getTotal());
        assertEquals("critical", result.getData().get(0).getSeverity());
    }

    @Test
    @DisplayName("分页查询 — 按启用状态筛选")
    void pageQueryWithEnabledFilter() {
        PageVulVO.Request request = new PageVulVO.Request();
        request.setEnabled(false);
        VulTemplate t = buildTemplate(1L, "CVE-001", "已禁用模板", "low", false, 1);
        stubSelectPage(List.of(t));

        ListResult<PageVulVO.Response> result = templateService.pageQuery(request);

        assertEquals(1, result.getTotal());
        assertFalse(result.getData().get(0).getEnabled());
    }

    @Test
    @DisplayName("分页查询 — 按分类筛选，分类下有模板")
    void pageQueryWithCategoryFilter() {
        PageVulVO.Request request = new PageVulVO.Request();
        request.setCategoryId(5L);

        VulCategoryMapping m1 = new VulCategoryMapping();
        m1.setTemplateId(1L);
        m1.setCategoryId(5L);
        when(mappingMapper.selectList(any())).thenReturn(List.of(m1));

        VulTemplate t = buildTemplate(1L, "CVE-001", "test", "low", true, 1);
        stubSelectPage(List.of(t));

        ListResult<PageVulVO.Response> result = templateService.pageQuery(request);

        assertEquals(1, result.getTotal());
    }

    @Test
    @DisplayName("分页查询 — 按分类筛选，分类下无模板，返回空")
    void pageQueryWithCategoryFilterEmpty() {
        PageVulVO.Request request = new PageVulVO.Request();
        request.setCategoryId(999L);
        when(mappingMapper.selectList(any())).thenReturn(Collections.emptyList());

        ListResult<PageVulVO.Response> result = templateService.pageQuery(request);

        assertAll("分类下无模板",
                () -> assertEquals(0, result.getTotal()),
                () -> assertTrue(result.getData().isEmpty())
        );
        verify(templateMapper, never()).selectPage(any(), any());
    }

    @Test
    @DisplayName("分页查询 — pageSize 上限 100")
    void pageQuerySizeCapped() {
        PageVulVO.Request request = new PageVulVO.Request();
        request.setPageSize(9999);

        when(templateMapper.selectPage(any(), any())).thenAnswer(inv -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<VulTemplate> page = inv.getArgument(0);
            assertEquals(100, page.getSize());
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        templateService.pageQuery(request);
    }

    @Test
    @DisplayName("分页查询 — tags 逗号分隔搜索")
    void pageQueryWithTagsFilter() {
        PageVulVO.Request request = new PageVulVO.Request();
        request.setTags("xss");

        VulTemplate t = buildTemplate(1L, "CVE-001", "XSS", "low", true, 1);
        t.setTags("xss,sqli,csrf");
        stubSelectPage(List.of(t));

        ListResult<PageVulVO.Response> result = templateService.pageQuery(request);

        assertEquals(1, result.getTotal());
    }

    // === getById ===

    @Test
    @DisplayName("根据 ID 查询 — 模板存在")
    void getByIdSuccess() {
        VulTemplate t = buildTemplate(1L, "CVE-001", "XSS 漏洞", "high", true, 2);
        t.setDescription("跨站脚本攻击");
        when(templateMapper.selectById(1L)).thenReturn(t);

        VulVO.Response response = templateService.getById(1L);

        assertAll("查询成功",
                () -> assertEquals(1L, response.getId()),
                () -> assertEquals("CVE-001", response.getTemplateId()),
                () -> assertEquals("XSS 漏洞", response.getName()),
                () -> assertEquals("high", response.getSeverity()),
                () -> assertEquals(2, response.getVersion()),
                () -> assertTrue(response.getEnabled())
        );
    }

    @Test
    @DisplayName("根据 ID 查询 — 模板不存在，抛 404")
    void getByIdNotFound() {
        when(templateMapper.selectById(anyLong())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> templateService.getById(999L));

        assertAll("模板不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("漏洞模板不存在", ex.getMessage())
        );
    }

    // === create ===

    @Test
    @DisplayName("创建模板 — enabled/version 为 null 时使用默认值")
    void createWithDefaults() {
        VulVO.Request request = new VulVO.Request();
        request.setTemplateId("CVE-NEW");
        request.setName("新漏洞");
        request.setSeverity("critical");

        when(templateMapper.insert((VulTemplate) any())).thenReturn(1);

        VulVO.Response response = templateService.create(request);

        assertAll("创建成功，默认值",
                () -> assertEquals("CVE-NEW", response.getTemplateId()),
                () -> assertTrue(response.getEnabled(), "enabled 为 null 时应默认 true"),
                () -> assertEquals(1, response.getVersion(), "version 为 null 时应默认 1")
        );
        verify(templateMapper).insert((VulTemplate) any());
    }

    @Test
    @DisplayName("创建模板 — 明确指定所有字段")
    void createWithExplicitValues() {
        VulVO.Request request = new VulVO.Request();
        request.setTemplateId("CVE-EXP");
        request.setName("明确指定");
        request.setSeverity("low");
        request.setEnabled(false);
        request.setVersion(5);

        when(templateMapper.insert((VulTemplate) any())).thenReturn(1);

        VulVO.Response response = templateService.create(request);

        assertAll("创建成功，保留指定值",
                () -> assertFalse(response.getEnabled()),
                () -> assertEquals(5, response.getVersion())
        );
    }

    // === update ===

    @Test
    @DisplayName("更新模板 — 部分字段更新成功")
    void updateSuccess() {
        VulVO.Request request = new VulVO.Request();
        request.setName("新名称");

        when(templateMapper.update(isNull(), any())).thenReturn(1);

        VulTemplate updated = buildTemplate(1L, "CVE-001", "新名称", "low", true, 1);
        when(templateMapper.selectById(1L)).thenReturn(updated);

        VulVO.Response response = templateService.update(1L, request);

        assertAll("部分更新成功",
                () -> assertEquals("新名称", response.getName()),
                () -> assertEquals(1L, response.getId())
        );
    }

    @Test
    @DisplayName("更新模板 — 所有字段为 null，抛 400")
    void updateAllFieldsNull() {
        VulVO.Request request = new VulVO.Request();

        ApiException ex = assertThrows(ApiException.class,
                () -> templateService.update(1L, request));

        assertAll("全 null 更新",
                () -> assertEquals(CommonErrorCode.PARAM_INVALID.getCode(), ex.getCode()),
                () -> assertEquals("至少需要提供一个更新字段", ex.getMessage())
        );
        verify(templateMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("更新模板 — 模板不存在，抛 404")
    void updateNotFound() {
        VulVO.Request request = new VulVO.Request();
        request.setName("ghost");

        when(templateMapper.update(isNull(), any())).thenReturn(0);

        ApiException ex = assertThrows(ApiException.class,
                () -> templateService.update(999L, request));

        assertAll("模板不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("漏洞模板不存在", ex.getMessage())
        );
        verify(templateMapper, never()).selectById(anyLong());
    }

    // === delete ===

    @Test
    @DisplayName("删除模板 — 无分类关联，删除成功")
    void deleteSuccess() {
        VulTemplate t = buildTemplate(1L, "CVE-DEL", "待删除", "low", true, 1);
        when(templateMapper.selectById(1L)).thenReturn(t);
        when(mappingMapper.selectCount(any())).thenReturn(0L);
        when(templateMapper.deleteById(1L)).thenReturn(1);

        templateService.delete(1L);

        verify(templateMapper).selectById(1L);
        verify(mappingMapper).selectCount(any());
        verify(templateMapper).deleteById(1L);
    }

    @Test
    @DisplayName("删除模板 — 模板不存在，抛 404")
    void deleteNotFound() {
        when(templateMapper.selectById(anyLong())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> templateService.delete(999L));

        assertAll("模板不存在",
                () -> assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode()),
                () -> assertEquals("漏洞模板不存在", ex.getMessage())
        );
        verify(templateMapper, never()).deleteById(anyLong());
        verify(mappingMapper, never()).selectCount(any());
    }

    @Test
    @DisplayName("删除模板 — 存在分类关联，抛 403")
    void deleteWithMappings() {
        VulTemplate t = buildTemplate(1L, "CVE-HAS", "有关联", "low", true, 1);
        when(templateMapper.selectById(1L)).thenReturn(t);
        when(mappingMapper.selectCount(any())).thenReturn(3L);

        ApiException ex = assertThrows(ApiException.class,
                () -> templateService.delete(1L));

        assertAll("存在分类关联",
                () -> assertEquals(CommonErrorCode.OPERATION_DENIED.getCode(), ex.getCode()),
                () -> assertEquals("该模板存在分类关联，请先移除所有关联再删除", ex.getMessage())
        );
        verify(templateMapper, never()).deleteById(anyLong());
    }

    // === getForDetection ===

    @Test
    @DisplayName("获取检测数据 — 仅返回检测引擎需要的字段")
    void getForDetectionSuccess() {
        VulTemplate t = new VulTemplate();
        t.setId(1L);
        t.setTemplateId("CVE-DETECT");
        t.setName("不需要的字段");
        t.setFlow("http(1)");
        t.setVariables("{\"url\":\"{{BaseURL}}\"}");
        t.setHttpRequests("[{\"method\":\"GET\"}]");
        t.setMatchers("[{\"type\":\"word\"}]");
        t.setExtractors("[{\"type\":\"regex\"}]");
        when(templateMapper.selectById(1L)).thenReturn(t);

        VulTemplateDetectDTO dto = templateService.getForDetection(1L);

        assertAll("仅返回检测字段",
                () -> assertEquals("CVE-DETECT", dto.getTemplateId()),
                () -> assertEquals("http(1)", dto.getFlow()),
                () -> assertEquals("[{\"method\":\"GET\"}]", dto.getHttpRequests()),
                () -> assertEquals("[{\"type\":\"word\"}]", dto.getMatchers()),
                () -> assertEquals("[{\"type\":\"regex\"}]", dto.getExtractors())
        );
    }

    @Test
    @DisplayName("获取检测数据 — 模板不存在，抛 404")
    void getForDetectionNotFound() {
        when(templateMapper.selectById(anyLong())).thenReturn(null);

        ApiException ex = assertThrows(ApiException.class,
                () -> templateService.getForDetection(999L));

        assertEquals(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), ex.getCode());
        assertEquals("漏洞模板不存在", ex.getMessage());
    }

    // === getByTemplateId ===

    @Test
    @DisplayName("按 templateId 查询 — 找到返回实体")
    void getByTemplateIdFound() {
        VulTemplate t = buildTemplate(1L, "CVE-FOUND", "test", "low", true, 1);
        when(templateMapper.selectOne(any())).thenReturn(t);

        VulTemplate result = templateService.getByTemplateId("CVE-FOUND");

        assertNotNull(result);
        assertEquals("CVE-FOUND", result.getTemplateId());
    }

    @Test
    @DisplayName("按 templateId 查询 — 未找到返回 null")
    void getByTemplateIdNotFound() {
        when(templateMapper.selectOne(any())).thenReturn(null);

        VulTemplate result = templateService.getByTemplateId("CVE-404");

        assertNull(result);
    }

    // === helper ===

    private VulTemplate buildTemplate(Long id, String templateId, String name,
                                       String severity, Boolean enabled, Integer version) {
        VulTemplate t = new VulTemplate();
        t.setId(id);
        t.setTemplateId(templateId);
        t.setName(name);
        t.setSeverity(severity);
        t.setEnabled(enabled);
        t.setVersion(version);
        return t;
    }

    @SuppressWarnings("unchecked")
    private void stubSelectPage(List<VulTemplate> templates) {
        when(templateMapper.selectPage(any(), any())).thenAnswer(inv -> {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<VulTemplate> page = inv.getArgument(0);
            page.setRecords(templates);
            page.setTotal(templates.size());
            return page;
        });
    }
}
