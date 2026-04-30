package com.hawkeye.vul.business.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.vul.business.mapper.*;
import com.hawkeye.vul.business.service.VulTemplateService;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import com.hawkeye.vul.common.pojo.entity.*;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateDetailVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplatePageQueryVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplatePageVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VulTemplateServiceImpl extends ServiceImpl<VulTemplateMapper, VulTemplate>
        implements VulTemplateService {

    private static final int PAGE_SIZE_MAX = 100;

    private final VulTagMapper tagMapper;
    private final VulCategoryMapper categoryMapper;
    private final VulTemplateTagMapper templateTagMapper;
    private final VulReferenceMapper referenceMapper;
    private final VulHttpStepMapper httpStepMapper;
    private final VulMatcherMapper matcherMapper;
    private final VulExtractorMapper extractorMapper;
    private final VulTemplateCategoryMapper templateCategoryMapper;

    // ── 分页查询 ──────────────────────────────────────

    @Override
    @LogExecutionTime
    public IPage<VulTemplatePageVO> pageQuery(VulTemplatePageQueryVO query) {
        int pageSize = Math.min(query.getSize() != null ? query.getSize() : 20, PAGE_SIZE_MAX);
        int pageNum = query.getPage() != null ? query.getPage() : 1;

        LambdaQueryWrapper<VulTemplate> wrapper = new LambdaQueryWrapper<VulTemplate>()
                .like(StrUtil.isNotBlank(query.getName()), VulTemplate::getName, query.getName())
                .eq(StrUtil.isNotBlank(query.getSeverity()), VulTemplate::getSeverity, query.getSeverity())
                .eq(query.getEnabled() != null, VulTemplate::getEnabled, query.getEnabled());

        // 标签过滤：JOIN vul_template_tag + vul_tag
        if (StrUtil.isNotBlank(query.getTag())) {
            Set<Long> tagTemplateIds = getTemplateIdsByTag(query.getTag());
            if (tagTemplateIds.isEmpty()) {
                return new Page<VulTemplatePageVO>(pageNum, pageSize, 0);
            }
            wrapper.in(VulTemplate::getId, tagTemplateIds);
        }

        // 分类过滤
        if (query.getCategoryId() != null) {
            Set<Long> catTemplateIds = templateCategoryMapper.selectList(
                    new LambdaQueryWrapper<VulTemplateCategory>()
                            .eq(VulTemplateCategory::getCategoryId, query.getCategoryId())
            ).stream().map(VulTemplateCategory::getTemplateId).collect(Collectors.toSet());
            if (catTemplateIds.isEmpty()) {
                return new Page<VulTemplatePageVO>(pageNum, pageSize, 0);
            }
            wrapper.in(VulTemplate::getId, catTemplateIds);
        }

        // 排序
        if ("name_asc".equals(query.getSort())) {
            wrapper.orderByAsc(VulTemplate::getName);
        } else if ("severity_asc".equals(query.getSort())) {
            wrapper.orderByAsc(VulTemplate::getSeverity);
        } else {
            wrapper.orderByDesc(VulTemplate::getCreateTime);
        }

        Page<VulTemplate> page = new Page<>(pageNum, pageSize);
        IPage<VulTemplate> result = baseMapper.selectPage(page, wrapper);

        List<VulTemplatePageVO> voList = result.getRecords().stream()
                .map(this::toPageVO)
                .toList();

        Page<VulTemplatePageVO> voPage = new Page<>(pageNum, pageSize, result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private VulTemplatePageVO toPageVO(VulTemplate template) {
        VulTemplatePageVO vo = new VulTemplatePageVO();
        vo.setId(template.getId());
        vo.setTemplateId(template.getTemplateId());
        vo.setName(template.getName());
        vo.setSeverity(template.getSeverity());
        vo.setCveId(template.getCveId());
        vo.setCvssScore(template.getCvssScore());
        vo.setEnabled(template.getEnabled());
        vo.setVersion(template.getVersion());
        vo.setCreateTime(template.getCreateTime());
        vo.setTags(getTagNames(template.getId()));
        vo.setCategories(getCategoryNames(template.getId()));
        return vo;
    }

    // ── 详情 ──────────────────────────────────────────

    @Override
    @LogExecutionTime
    public VulTemplateDetailVO getDetail(Long id) {
        VulTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "漏洞模板不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        return assembleDetail(template);
    }

    private VulTemplateDetailVO assembleDetail(VulTemplate template) {
        Long id = template.getId();
        VulTemplateDetailVO vo = new VulTemplateDetailVO();
        vo.setId(id);
        vo.setTemplateId(template.getTemplateId());
        vo.setName(template.getName());
        vo.setDescription(template.getDescription());
        vo.setAuthor(template.getAuthor());
        vo.setSeverity(template.getSeverity());
        vo.setCveId(template.getCveId());
        vo.setCweId(template.getCweId());
        vo.setCvssScore(template.getCvssScore());
        vo.setEpssScore(template.getEpssScore());
        vo.setFlow(template.getFlow());
        vo.setVariables(parseJson(template.getVariables()));
        vo.setEnabled(template.getEnabled());
        vo.setVersion(template.getVersion());
        vo.setCreateTime(template.getCreateTime());
        vo.setUpdateTime(template.getUpdateTime());

        vo.setTags(getTagNames(id));
        vo.setReferences(getReferenceVOs(id));
        vo.setCategories(getCategoryBriefVOs(id));
        vo.setHttpSteps(getHttpStepVOs(id));
        return vo;
    }

    // ── 创建 ──────────────────────────────────────────

    @Override
    @LogExecutionTime
    @Transactional
    public Long create(VulTemplateRequestVO request) {
        VulTemplate template = new VulTemplate();
        template.setTemplateId(request.getTemplateId().toLowerCase());
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setAuthor(request.getAuthor());
        template.setSeverity(request.getSeverity());
        template.setCveId(request.getCveId());
        template.setCweId(request.getCweId());
        template.setCvssScore(request.getCvssScore());
        template.setEpssScore(request.getEpssScore());
        template.setFlow(request.getFlow());
        template.setVariables(toJson(request.getVariables()));
        template.setEnabled(true);
        template.setVersion(1);

        baseMapper.insert(template);
        Long templateId = template.getId();

        // 级联写入子表
        saveTags(templateId, request.getTags());
        saveReferences(templateId, request.getReferences());
        saveHttpSteps(templateId, request.getHttpSteps());
        saveCategories(templateId, request.getCategoryIds());

        return templateId;
    }

    // ── 更新 ──────────────────────────────────────────

    @Override
    @LogExecutionTime
    @Transactional
    public void update(Long id, VulTemplateRequestVO request) {
        VulTemplate template = baseMapper.selectById(id);
        if (template == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "漏洞模板不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        LambdaUpdateWrapper<VulTemplate> wrapper = new LambdaUpdateWrapper<VulTemplate>()
                .eq(VulTemplate::getId, id)
                .set(request.getName() != null, VulTemplate::getName, request.getName())
                .set(request.getDescription() != null, VulTemplate::getDescription, request.getDescription())
                .set(request.getAuthor() != null, VulTemplate::getAuthor, request.getAuthor())
                .set(request.getSeverity() != null, VulTemplate::getSeverity, request.getSeverity())
                .set(request.getCveId() != null, VulTemplate::getCveId, request.getCveId())
                .set(request.getCweId() != null, VulTemplate::getCweId, request.getCweId())
                .set(request.getCvssScore() != null, VulTemplate::getCvssScore, request.getCvssScore())
                .set(request.getEpssScore() != null, VulTemplate::getEpssScore, request.getEpssScore())
                .set(request.getFlow() != null, VulTemplate::getFlow, request.getFlow())
                .set(request.getVariables() != null, VulTemplate::getVariables, toJson(request.getVariables()));

        if (allFieldsNull(request)) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(),
                    "至少需要提供一个更新字段", HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        baseMapper.update(null, wrapper);

        // 子表：先删后插
        if (request.getTags() != null) {
            templateTagMapper.delete(new LambdaQueryWrapper<VulTemplateTag>()
                    .eq(VulTemplateTag::getTemplateId, id));
            saveTags(id, request.getTags());
        }
        if (request.getReferences() != null) {
            referenceMapper.delete(new LambdaQueryWrapper<VulReference>()
                    .eq(VulReference::getTemplateId, id));
            saveReferences(id, request.getReferences());
        }
        if (request.getHttpSteps() != null) {
            httpStepMapper.delete(new LambdaQueryWrapper<VulHttpStep>()
                    .eq(VulHttpStep::getTemplateId, id));
            matcherMapper.delete(new LambdaQueryWrapper<VulMatcher>()
                    .eq(VulMatcher::getTemplateId, id));
            extractorMapper.delete(new LambdaQueryWrapper<VulExtractor>()
                    .eq(VulExtractor::getTemplateId, id));
            saveHttpSteps(id, request.getHttpSteps());
        }
        if (request.getCategoryIds() != null) {
            templateCategoryMapper.delete(new LambdaQueryWrapper<VulTemplateCategory>()
                    .eq(VulTemplateCategory::getTemplateId, id));
            saveCategories(id, request.getCategoryIds());
        }
    }

    // ── 删除 ──────────────────────────────────────────

    @Override
    @LogExecutionTime
    @Transactional
    public void delete(Long id) {
        if (baseMapper.selectById(id) == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "漏洞模板不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        long mappingCount = templateCategoryMapper.selectCount(
                new LambdaQueryWrapper<VulTemplateCategory>()
                        .eq(VulTemplateCategory::getTemplateId, id));
        if (mappingCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该模板存在分类关联，请先移除所有关联再删除",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        // 级联删除子表
        templateTagMapper.delete(new LambdaQueryWrapper<VulTemplateTag>().eq(VulTemplateTag::getTemplateId, id));
        referenceMapper.delete(new LambdaQueryWrapper<VulReference>().eq(VulReference::getTemplateId, id));
        matcherMapper.delete(new LambdaQueryWrapper<VulMatcher>().eq(VulMatcher::getTemplateId, id));
        extractorMapper.delete(new LambdaQueryWrapper<VulExtractor>().eq(VulExtractor::getTemplateId, id));
        httpStepMapper.delete(new LambdaQueryWrapper<VulHttpStep>().eq(VulHttpStep::getTemplateId, id));
        baseMapper.deleteById(id);
    }

    @Override
    @LogExecutionTime
    @Transactional
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            try {
                delete(id);
            } catch (ApiException ignored) {
                // 跳过有分类关联的
            }
        }
    }

    @Override
    @LogExecutionTime
    @Transactional
    public void setEnabled(Long id, Boolean enabled) {
        baseMapper.update(null,
                new LambdaUpdateWrapper<VulTemplate>()
                        .eq(VulTemplate::getId, id)
                        .set(VulTemplate::getEnabled, enabled));
    }

    // ── Feign 检测接口 ─────────────────────────────────

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
        dto.setVariables(parseJson(template.getVariables()));

        List<VulHttpStep> steps = httpStepMapper.selectList(
                new LambdaQueryWrapper<VulHttpStep>()
                        .eq(VulHttpStep::getTemplateId, id)
                        .orderByAsc(VulHttpStep::getStepOrder));

        List<VulTemplateDetectDTO.HttpStepDetect> stepDTOs = new ArrayList<>();
        for (VulHttpStep step : steps) {
            VulTemplateDetectDTO.HttpStepDetect sd = new VulTemplateDetectDTO.HttpStepDetect();
            sd.setStepOrder(step.getStepOrder());
            sd.setMethod(step.getMethod());
            sd.setPath(parseJsonList(step.getPath()));
            sd.setHeaders(parseJsonMap(step.getHeaders()));
            sd.setBody(step.getBody());
            sd.setRaw(step.getRaw());
            sd.setAttack(step.getAttack());
            sd.setMatchersCondition(step.getMatchersCondition());

            List<VulMatcher> matchers = matcherMapper.selectList(
                    new LambdaQueryWrapper<VulMatcher>()
                            .eq(VulMatcher::getTemplateId, id)
                            .and(w -> w.eq(VulMatcher::getStepOrder, step.getStepOrder())
                                    .or().isNull(VulMatcher::getStepOrder)));
            sd.setMatchers(matchers.stream().map(m -> {
                VulTemplateDetectDTO.MatcherDetect md = new VulTemplateDetectDTO.MatcherDetect();
                md.setType(m.getType()); md.setPart(m.getPart()); md.setCondition(m.getCondition());
                md.setNegative(m.getNegative()); md.setCaseInsensitive(m.getCaseInsensitive());
                md.setConfig(parseJson(m.getConfig()));
                return md;
            }).toList());

            List<VulExtractor> extractors = extractorMapper.selectList(
                    new LambdaQueryWrapper<VulExtractor>()
                            .eq(VulExtractor::getTemplateId, id)
                            .and(w -> w.eq(VulExtractor::getStepOrder, step.getStepOrder())
                                    .or().isNull(VulExtractor::getStepOrder)));
            sd.setExtractors(extractors.stream().map(e -> {
                VulTemplateDetectDTO.ExtractorDetect ed = new VulTemplateDetectDTO.ExtractorDetect();
                ed.setType(e.getType()); ed.setPart(e.getPart()); ed.setName(e.getName());
                ed.setConfig(parseJson(e.getConfig()));
                ed.setInternal(e.getInternal()); ed.setGroupNum(e.getGroupNum());
                return ed;
            }).toList());

            stepDTOs.add(sd);
        }
        dto.setHttpSteps(stepDTOs);
        return dto;
    }

    @Override
    public VulTemplate getByTemplateId(String templateId) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<VulTemplate>()
                        .eq(VulTemplate::getTemplateId, templateId));
    }

    // ── 子表操作 ──────────────────────────────────────

    private void saveTags(Long templateId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;
        for (String name : tagNames) {
            String lower = name.toLowerCase();
            VulTag tag = tagMapper.selectOne(
                    new LambdaQueryWrapper<VulTag>().eq(VulTag::getName, lower));
            if (tag == null) {
                tag = new VulTag();
                tag.setName(lower);
                tagMapper.insert(tag);
            }
            VulTemplateTag tt = new VulTemplateTag();
            tt.setTemplateId(templateId);
            tt.setTagId(tag.getId());
            templateTagMapper.insert(tt);
        }
    }

    private void saveReferences(Long templateId, List<VulTemplateRequestVO.ReferenceRequest> refs) {
        if (refs == null || refs.isEmpty()) return;
        for (VulTemplateRequestVO.ReferenceRequest r : refs) {
            VulReference ref = new VulReference();
            ref.setTemplateId(templateId);
            ref.setUrl(r.getUrl());
            ref.setTitle(r.getTitle());
            referenceMapper.insert(ref);
        }
    }

    private void saveHttpSteps(Long templateId, List<VulTemplateRequestVO.HttpStepRequest> steps) {
        if (steps == null || steps.isEmpty()) return;
        int order = 1;
        for (VulTemplateRequestVO.HttpStepRequest s : steps) {
            VulHttpStep step = new VulHttpStep();
            step.setTemplateId(templateId);
            step.setStepOrder(order);
            step.setMethod(s.getMethod());
            step.setPath(toJson(s.getPath()));
            step.setHeaders(toJson(s.getHeaders()));
            step.setBody(s.getBody());
            step.setRaw(s.getRaw());
            step.setAttack(s.getAttack());
            step.setMatchersCondition(s.getMatchersCondition() != null ? s.getMatchersCondition() : "or");
            step.setStopAtFirstMatch(s.getStopAtFirstMatch() != null ? s.getStopAtFirstMatch() : false);
            httpStepMapper.insert(step);

            // 步骤级 matchers / extractors
            if (s.getMatchers() != null) {
                for (VulTemplateRequestVO.MatcherRequest m : s.getMatchers()) {
                    VulMatcher matcher = new VulMatcher();
                    matcher.setTemplateId(templateId);
                    matcher.setStepOrder(order);
                    matcher.setType(m.getType());
                    matcher.setPart(m.getPart());
                    matcher.setCondition(m.getCondition() != null ? m.getCondition() : "or");
                    matcher.setNegative(m.getNegative() != null ? m.getNegative() : false);
                    matcher.setCaseInsensitive(m.getCaseInsensitive() != null ? m.getCaseInsensitive() : false);
                    matcher.setInternal(m.getInternal() != null ? m.getInternal() : false);
                    matcher.setName(m.getName());
                    matcher.setConfig(toJson(m.getConfig()));
                    matcherMapper.insert(matcher);
                }
            }
            if (s.getExtractors() != null) {
                for (VulTemplateRequestVO.ExtractorRequest e : s.getExtractors()) {
                    VulExtractor extr = new VulExtractor();
                    extr.setTemplateId(templateId);
                    extr.setStepOrder(order);
                    extr.setType(e.getType());
                    extr.setPart(e.getPart());
                    extr.setName(e.getName());
                    extr.setConfig(toJson(e.getConfig()));
                    extr.setInternal(e.getInternal() != null ? e.getInternal() : false);
                    extr.setGroupNum(e.getGroupNum() != null ? e.getGroupNum() : 1);
                    extractorMapper.insert(extr);
                }
            }
            order++;
        }
    }

    private void saveCategories(Long templateId, List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return;
        for (Long catId : categoryIds) {
            VulTemplateCategory tc = new VulTemplateCategory();
            tc.setTemplateId(templateId);
            tc.setCategoryId(catId);
            templateCategoryMapper.insert(tc);
        }
    }

    // ── 详情组装帮助方法 ──────────────────────────────

    private List<String> getTagNames(Long templateId) {
        List<Long> tagIds = templateTagMapper.selectList(
                new LambdaQueryWrapper<VulTemplateTag>()
                        .eq(VulTemplateTag::getTemplateId, templateId)
        ).stream().map(VulTemplateTag::getTagId).toList();
        if (tagIds.isEmpty()) return Collections.emptyList();
        return tagMapper.selectBatchIds(tagIds).stream()
                .map(VulTag::getName).toList();
    }

    private List<VulTemplateDetailVO.ReferenceVO> getReferenceVOs(Long templateId) {
        return referenceMapper.selectList(
                new LambdaQueryWrapper<VulReference>()
                        .eq(VulReference::getTemplateId, templateId)
        ).stream().map(r -> {
            VulTemplateDetailVO.ReferenceVO rv = new VulTemplateDetailVO.ReferenceVO();
            rv.setUrl(r.getUrl());
            rv.setTitle(r.getTitle());
            return rv;
        }).toList();
    }

    private List<VulTemplateDetailVO.CategoryBriefVO> getCategoryBriefVOs(Long templateId) {
        List<Long> catIds = templateCategoryMapper.selectList(
                new LambdaQueryWrapper<VulTemplateCategory>()
                        .eq(VulTemplateCategory::getTemplateId, templateId)
        ).stream().map(VulTemplateCategory::getCategoryId).toList();
        if (catIds.isEmpty()) return Collections.emptyList();
        return categoryMapper.selectBatchIds(catIds).stream()
                .map(c -> {
                    VulTemplateDetailVO.CategoryBriefVO cv = new VulTemplateDetailVO.CategoryBriefVO();
                    cv.setCategoryId(c.getCategoryId());
                    cv.setName(c.getName());
                    return cv;
                }).toList();
    }

    private List<String> getCategoryNames(Long templateId) {
        return getCategoryBriefVOs(templateId).stream()
                .map(c -> c.getName()).toList();
    }

    private List<VulTemplateDetailVO.HttpStepVO> getHttpStepVOs(Long templateId) {
        List<VulHttpStep> steps = httpStepMapper.selectList(
                new LambdaQueryWrapper<VulHttpStep>()
                        .eq(VulHttpStep::getTemplateId, templateId)
                        .orderByAsc(VulHttpStep::getStepOrder));
        return steps.stream().map(step -> {
            VulTemplateDetailVO.HttpStepVO sv = new VulTemplateDetailVO.HttpStepVO();
            sv.setStepOrder(step.getStepOrder());
            sv.setMethod(step.getMethod());
            sv.setPath(parseJsonList(step.getPath()));
            sv.setHeaders(parseJsonMap(step.getHeaders()));
            sv.setBody(step.getBody());
            sv.setRaw(step.getRaw());
            sv.setAttack(step.getAttack());
            sv.setMatchersCondition(step.getMatchersCondition());
            sv.setStopAtFirstMatch(step.getStopAtFirstMatch());
            sv.setMatchers(getMatcherVOs(templateId, step.getStepOrder()));
            sv.setExtractors(getExtractorVOs(templateId, step.getStepOrder()));
            return sv;
        }).toList();
    }

    private List<VulTemplateDetailVO.MatcherVO> getMatcherVOs(Long templateId, Integer stepOrder) {
        return matcherMapper.selectList(
                new LambdaQueryWrapper<VulMatcher>()
                        .eq(VulMatcher::getTemplateId, templateId)
                        .and(w -> w.eq(VulMatcher::getStepOrder, stepOrder)
                                .or().isNull(VulMatcher::getStepOrder))
        ).stream().map(m -> {
            VulTemplateDetailVO.MatcherVO mv = new VulTemplateDetailVO.MatcherVO();
            mv.setType(m.getType()); mv.setPart(m.getPart()); mv.setCondition(m.getCondition());
            mv.setNegative(m.getNegative()); mv.setCaseInsensitive(m.getCaseInsensitive());
            mv.setInternal(m.getInternal()); mv.setName(m.getName());
            mv.setConfig(parseJson(m.getConfig()));
            return mv;
        }).toList();
    }

    private List<VulTemplateDetailVO.ExtractorVO> getExtractorVOs(Long templateId, Integer stepOrder) {
        return extractorMapper.selectList(
                new LambdaQueryWrapper<VulExtractor>()
                        .eq(VulExtractor::getTemplateId, templateId)
                        .and(w -> w.eq(VulExtractor::getStepOrder, stepOrder)
                                .or().isNull(VulExtractor::getStepOrder))
        ).stream().map(e -> {
            VulTemplateDetailVO.ExtractorVO ev = new VulTemplateDetailVO.ExtractorVO();
            ev.setType(e.getType()); ev.setPart(e.getPart()); ev.setName(e.getName());
            ev.setConfig(parseJson(e.getConfig()));
            ev.setInternal(e.getInternal()); ev.setGroupNum(e.getGroupNum());
            return ev;
        }).toList();
    }

    private Set<Long> getTemplateIdsByTag(String tagName) {
        List<VulTag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<VulTag>().eq(VulTag::getName, tagName.toLowerCase()));
        if (tags.isEmpty()) return Collections.emptySet();
        return templateTagMapper.selectList(
                new LambdaQueryWrapper<VulTemplateTag>()
                        .eq(VulTemplateTag::getTagId, tags.get(0).getId())
        ).stream().map(VulTemplateTag::getTemplateId).collect(Collectors.toSet());
    }

    // ── JSON 工具 ────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (StrUtil.isBlank(json)) return null;
        try { return JSONUtil.toBean(json, Map.class); } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJsonMap(String json) {
        if (StrUtil.isBlank(json)) return null;
        try { return JSONUtil.toBean(json, Map.class); } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonList(String json) {
        if (StrUtil.isBlank(json)) return null;
        try { return JSONUtil.toList(json, String.class); } catch (Exception e) { return null; }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        return JSONUtil.toJsonStr(obj);
    }

    private boolean allFieldsNull(VulTemplateRequestVO req) {
        return req.getName() == null && req.getDescription() == null
                && req.getAuthor() == null && req.getSeverity() == null
                && req.getCveId() == null && req.getCweId() == null
                && req.getCvssScore() == null && req.getEpssScore() == null
                && req.getFlow() == null && req.getVariables() == null
                && req.getTags() == null && req.getReferences() == null
                && req.getHttpSteps() == null && req.getCategoryIds() == null;
    }
}
