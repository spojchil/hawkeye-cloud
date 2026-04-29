package com.hawkeye.vul.business.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.common.utils.response.ListResult;
import com.hawkeye.vul.business.mapstruct.VulMapstruct;
import com.hawkeye.vul.business.mapper.VulCategoryMappingMapper;
import com.hawkeye.vul.business.mapper.VulTemplateMapper;
import com.hawkeye.vul.business.service.VulTemplateService;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import com.hawkeye.vul.common.pojo.entity.VulCategoryMapping;
import com.hawkeye.vul.common.pojo.entity.VulTemplate;
import com.hawkeye.vul.common.pojo.vo.vul.PageVulVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * 漏洞模板服务实现。
 * <p>
 * 负责漏洞检测模板的 CRUD 及分页查询。
 * 模板数据来源于 Nuclei YAML（~10,688 个），仅保留 http 协议模板。
 * {@code templateId} 是 YAML 的业务唯一标识，{@code id} 是数据库自增主键。
 * <p>
 * <b>关键业务规则：</b>
 * <ul>
 *   <li><b>分页查询：</b> 支持按名称、严重程度、标签、启用状态、分类筛选</li>
 *   <li><b>删除保护：</b> 已关联分类的模板不允许删除，需先移除所有关联</li>
 *   <li><b>检测接口：</b> getForDetection 仅返回检测引擎需要的字段</li>
 * </ul>
 *
 * @see VulTemplateService
 */
@Service
@RequiredArgsConstructor
public class VulTemplateServiceImpl extends ServiceImpl<VulTemplateMapper, VulTemplate>
        implements VulTemplateService {

    private static final int PAGE_SIZE_MAX = 100;

    private final VulMapstruct vulMapstruct;
    private final VulCategoryMappingMapper mappingMapper;

    @Override
    @LogExecutionTime
    public ListResult<PageVulVO.Response> pageQuery(PageVulVO.Request request) {
        int pageSize = Math.min(request.getPageSize() != null ? request.getPageSize() : 10, PAGE_SIZE_MAX);
        int pageNum = request.getPage() != null ? request.getPage() : 1;

        LambdaQueryWrapper<VulTemplate> wrapper = new LambdaQueryWrapper<VulTemplate>()
                .like(StrUtil.isNotBlank(request.getName()), VulTemplate::getName, request.getName())
                .eq(StrUtil.isNotBlank(request.getSeverity()), VulTemplate::getSeverity, request.getSeverity())
                .eq(request.getEnabled() != null, VulTemplate::getEnabled, request.getEnabled())
                .orderByDesc(VulTemplate::getCreateTime);

        if (StrUtil.isNotBlank(request.getTags())) {
            String keyword = request.getTags().trim();
            wrapper.and(w -> w.like(VulTemplate::getTags, keyword + ",%")
                    .or().like(VulTemplate::getTags, "%," + keyword)
                    .or().like(VulTemplate::getTags, "%," + keyword + ",%")
                    .or().eq(VulTemplate::getTags, keyword));
        }

        if (request.getCategoryId() != null) {
            List<Long> templateIds = mappingMapper.selectList(
                    new LambdaQueryWrapper<VulCategoryMapping>()
                            .eq(VulCategoryMapping::getCategoryId, request.getCategoryId())
                            .select(VulCategoryMapping::getTemplateId)
            ).stream().map(VulCategoryMapping::getTemplateId).distinct().toList();

            if (templateIds.isEmpty()) {
                return ListResult.result(0, Collections.emptyList());
            }
            wrapper.in(VulTemplate::getId, templateIds);
        }

        Page<VulTemplate> page = new Page<>(pageNum, pageSize);
        IPage<VulTemplate> result = baseMapper.selectPage(page, wrapper);

        List<PageVulVO.Response> voList = result.getRecords()
                .stream()
                .map(vulMapstruct::toPageVO)
                .toList();

        return ListResult.result((int) result.getTotal(), voList);
    }

    @Override
    @LogExecutionTime
    public VulVO.Response getById(Long id) {
        VulTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "漏洞模板不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        return vulMapstruct.toResponseVO(template);
    }

    @Override
    @LogExecutionTime
    @Transactional
    public VulVO.Response create(VulVO.Request request) {
        VulTemplate template = vulMapstruct.toEntity(request);
        if (template.getEnabled() == null) {
            template.setEnabled(true);
        }
        if (template.getVersion() == null) {
            template.setVersion(1);
        }
        baseMapper.insert(template);
        return vulMapstruct.toResponseVO(template);
    }

    @Override
    @LogExecutionTime
    @Transactional
    public VulVO.Response update(Long id, VulVO.Request request) {
        if (allFieldsNull(request)) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(),
                    "至少需要提供一个更新字段", HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        LambdaUpdateWrapper<VulTemplate> wrapper = new LambdaUpdateWrapper<VulTemplate>()
                .eq(VulTemplate::getId, id)
                .set(request.getName() != null, VulTemplate::getName, request.getName())
                .set(request.getDescription() != null, VulTemplate::getDescription, request.getDescription())
                .set(request.getAuthor() != null, VulTemplate::getAuthor, request.getAuthor())
                .set(request.getSeverity() != null, VulTemplate::getSeverity, request.getSeverity())
                .set(request.getTags() != null, VulTemplate::getTags, request.getTags())
                .set(request.getReference() != null, VulTemplate::getReference, request.getReference())
                .set(request.getClassification() != null, VulTemplate::getClassification,
                        request.getClassification())
                .set(request.getMetadata() != null, VulTemplate::getMetadata, request.getMetadata())
                .set(request.getFlow() != null, VulTemplate::getFlow, request.getFlow())
                .set(request.getVariables() != null, VulTemplate::getVariables, request.getVariables())
                .set(request.getHttpRequests() != null, VulTemplate::getHttpRequests,
                        request.getHttpRequests())
                .set(request.getMatchers() != null, VulTemplate::getMatchers, request.getMatchers())
                .set(request.getExtractors() != null, VulTemplate::getExtractors, request.getExtractors())
                .set(request.getEnabled() != null, VulTemplate::getEnabled, request.getEnabled())
                .set(request.getVersion() != null, VulTemplate::getVersion, request.getVersion());

        if (baseMapper.update(null, wrapper) == 0) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "漏洞模板不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        VulTemplate updated = baseMapper.selectById(id);
        return vulMapstruct.toResponseVO(updated);
    }

    @Override
    @LogExecutionTime
    @Transactional
    public void delete(Long id) {
        VulTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "漏洞模板不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        long mappingCount = mappingMapper.selectCount(
                new LambdaQueryWrapper<VulCategoryMapping>()
                        .eq(VulCategoryMapping::getTemplateId, id));
        if (mappingCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该模板存在分类关联，请先移除所有关联再删除",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        baseMapper.deleteById(id);
    }

    @Override
    @LogExecutionTime
    public VulTemplateDetectDTO getForDetection(Long id) {
        VulTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "漏洞模板不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        VulTemplateDetectDTO dto = new VulTemplateDetectDTO();
        dto.setTemplateId(template.getTemplateId());
        dto.setFlow(template.getFlow());
        dto.setVariables(template.getVariables());
        dto.setHttpRequests(template.getHttpRequests());
        dto.setMatchers(template.getMatchers());
        dto.setExtractors(template.getExtractors());
        return dto;
    }

    @Override
    public VulTemplate getByTemplateId(String templateId) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<VulTemplate>()
                        .eq(VulTemplate::getTemplateId, templateId));
    }

    /**
     * 判断 request 中所有字段是否全为 null。
     * 全 null 时禁止更新，避免生成无意义的 SQL。
     */
    private boolean allFieldsNull(VulVO.Request request) {
        return request.getName() == null
                && request.getDescription() == null
                && request.getAuthor() == null
                && request.getSeverity() == null
                && request.getTags() == null
                && request.getReference() == null
                && request.getClassification() == null
                && request.getMetadata() == null
                && request.getFlow() == null
                && request.getVariables() == null
                && request.getHttpRequests() == null
                && request.getMatchers() == null
                && request.getExtractors() == null
                && request.getEnabled() == null
                && request.getVersion() == null;
    }
}
