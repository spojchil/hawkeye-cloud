package com.hawkeye.vul.business.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.common.utils.response.ListResult;
import com.hawkeye.vul.business.mapstruct.VulTemplateMapstruct;
import com.hawkeye.vul.business.mapper.*;
import com.hawkeye.vul.business.service.VulTemplateService;
import com.hawkeye.vul.common.pojo.dto.VulTemplateDetectDTO;
import com.hawkeye.vul.common.pojo.entity.*;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplatePageVO;
import com.hawkeye.vul.common.pojo.vo.vul.VulTemplateVO;
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
    private static final Set<String> VALID_SEVERITIES =
            Set.of("critical", "high", "medium", "low", "info", "unknown");
    private static final Set<String> MATCHER_CONFIG_KEYS =
            Set.of("words", "regex", "status", "size", "dsl", "xpath", "binary");
    private static final Set<String> EXTRACTOR_CONFIG_KEYS =
            Set.of("regex", "kval", "json", "dsl", "xpath");

    private final VulTemplateMapstruct templateMapstruct;
    private final VulTagMapper tagMapper;
    private final VulCategoryMapper categoryMapper;
    private final VulTemplateTagMapper templateTagMapper;
    private final VulTemplateCategoryMapper templateCategoryMapper;
    private final VulReferenceMapper referenceMapper;
    private final VulHttpStepMapper httpStepMapper;
    private final VulMatcherMapper matcherMapper;
    private final VulExtractorMapper extractorMapper;
    private final VulTextContentMapper textContentMapper;

    // ── 分页 ──────────────────────────────────────────

    @Override
    @LogExecutionTime("模板分页查询")
    public ListResult<VulTemplatePageVO.Response> pageQuery(VulTemplatePageVO.Request request) {
        int pageSize = Math.min(request.getPageSize() != null ? request.getPageSize() : 20, PAGE_SIZE_MAX);
        int pageNum = request.getPage() != null ? request.getPage() : 1;

        LambdaQueryWrapper<VulTemplate> wrapper = new LambdaQueryWrapper<VulTemplate>()
                .eq(VulTemplate::getDeletedAt, 0L)
                .like(StrUtil.isNotBlank(request.getName()), VulTemplate::getName, request.getName())
                .eq(StrUtil.isNotBlank(request.getSeverity()), VulTemplate::getSeverity, request.getSeverity())
                .eq(request.getEnabled() != null, VulTemplate::getEnabled, request.getEnabled())
                .orderByDesc(VulTemplate::getCreateTime);

        if (StrUtil.isNotBlank(request.getTag())) {
            Set<Long> tagTemplateIds = getTemplateIdsByTag(request.getTag());
            if (tagTemplateIds.isEmpty()) {
                return ListResult.result(0, Collections.emptyList());
            }
            wrapper.in(VulTemplate::getTemplateId, tagTemplateIds);
        }

        if (request.getCategoryId() != null) {
            List<Long> catTemplateIds = templateCategoryMapper.selectList(
                    new LambdaQueryWrapper<VulTemplateCategory>()
                            .eq(VulTemplateCategory::getDeletedAt, 0L)
                            .eq(VulTemplateCategory::getCategoryId, request.getCategoryId())
            ).stream().map(VulTemplateCategory::getTemplateId).toList();
            if (catTemplateIds.isEmpty()) {
                return ListResult.result(0, Collections.emptyList());
            }
            wrapper.in(VulTemplate::getTemplateId, catTemplateIds);
        }

        Page<VulTemplate> page = new Page<>(pageNum, pageSize);
        Page<VulTemplate> result = baseMapper.selectPage(page, wrapper);

        List<VulTemplatePageVO.Response> voList = result.getRecords().stream()
                .map(t -> {
                    VulTemplatePageVO.Response vo = templateMapstruct.toPageVO(t);
                    vo.setTags(getTagNames(t.getTemplateId()));
                    vo.setCategories(getCategoryNames(t.getTemplateId()));
                    return vo;
                })
                .toList();

        return ListResult.result((int) result.getTotal(), voList);
    }

    // ── 详情 ──────────────────────────────────────────

    @Override
    @LogExecutionTime("查询模板详情")
    public VulTemplateVO.Response getDetail(Long templateId) {
        VulTemplate template = baseMapper.selectOne(
                new LambdaQueryWrapper<VulTemplate>()
                        .eq(VulTemplate::getTemplateId, templateId)
                        .eq(VulTemplate::getDeletedAt, 0L));
        if (template == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "漏洞模板不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        return assembleDetail(template);
    }

    // ── 删除 ──────────────────────────────────────────

    @Override
    @LogExecutionTime("删除模板")
    @Transactional
    public void delete(Long templateId) {
        VulTemplate template = baseMapper.selectOne(
                new LambdaQueryWrapper<VulTemplate>()
                        .eq(VulTemplate::getTemplateId, templateId)
                        .eq(VulTemplate::getDeletedAt, 0L));
        if (template == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "漏洞模板不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        long now = System.currentTimeMillis();
        softDeleteByTemplateId(templateId, now);

        baseMapper.update(null,
                new LambdaUpdateWrapper<VulTemplate>()
                        .eq(VulTemplate::getTemplateId, templateId)
                        .set(VulTemplate::getDeletedAt, now));
    }

    // ── 导入模板（JSON 解析 → 级联创建） ──────────────

    @Override
    @LogExecutionTime("导入模板")
    @Transactional
    @SuppressWarnings("unchecked")
    public VulTemplateVO.Response importTemplate(Map<String, Object> templateJson, List<Long> categoryIds) {
        // 1. 解析顶层信息
        String yamlId = str(templateJson.get("id")).toLowerCase();
        if (yamlId.isEmpty()) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(),
                    "模板 id 不能为空", HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        // 同名覆盖：先软删除旧模板
        VulTemplate existing = baseMapper.selectOne(
                new LambdaQueryWrapper<VulTemplate>()
                        .eq(VulTemplate::getDeletedAt, 0L)
                        .eq(VulTemplate::getYamlId, yamlId));
        if (existing != null) {
            long now = System.currentTimeMillis();
            softDeleteByTemplateId(existing.getTemplateId(), now);
            baseMapper.update(null,
                    new LambdaUpdateWrapper<VulTemplate>()
                            .eq(VulTemplate::getTemplateId, existing.getTemplateId())
                            .set(VulTemplate::getDeletedAt, now));
        }

        Map<String, Object> info = map(templateJson.get("info"));
        Map<String, Object> classification = map(info.get("classification"));

        VulTemplate template = new VulTemplate();
        template.setYamlId(yamlId);
        template.setName(str(info.get("name"), yamlId));
        template.setAuthor(str(info.get("author")));
        template.setDescription(str(info.get("description")));
        template.setImpact(str(info.get("impact")));
        template.setSeverity(normalizeSeverity(str(info.get("severity"))));
        template.setMetadata(toJson(info.get("metadata")));
        template.setCveId(str(classification.get("cve-id")));
        template.setCweId(str(classification.get("cwe-id")));
        template.setCvssMetrics(str(classification.get("cvss-metrics")));
        template.setCvssScore(dbl(classification.get("cvss-score")));
        template.setEpssScore(dbl(classification.get("epss-score")));
        template.setCpe(str(classification.get("cpe")));
        template.setRemediation(str(info.get("remediation")));
        template.setFlow(str(templateJson.get("flow")));
        template.setVariables(toJson(templateJson.get("variables")));
        template.setEnabled(true);

        baseMapper.insert(template);
        Long templateId = template.getTemplateId();

        // 2. 标签
        saveTags(templateId, parseTagList(info.get("tags")));

        // 3. 参考链接
        saveReferences(templateId, info.get("reference"));

        // 4. 分类关联
        if (categoryIds != null && !categoryIds.isEmpty()) {
            saveCategories(templateId, categoryIds);
        }

        // 5. HTTP 步骤
        Object httpObj = templateJson.get("http");
        if (httpObj instanceof List<?> steps) {
            saveHttpSteps(templateId, steps);
        }

        return assembleDetail(baseMapper.selectById(templateId));
    }

    // ── Feign 检测接口 ─────────────────────────────────

    @Override
    @LogExecutionTime("获取检测配置")
    public VulTemplateDetectDTO getForDetection(Long templateId) {
        VulTemplate template = baseMapper.selectOne(
                new LambdaQueryWrapper<VulTemplate>()
                        .eq(VulTemplate::getTemplateId, templateId)
                        .eq(VulTemplate::getDeletedAt, 0L));
        if (template == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "漏洞模板不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        VulTemplateDetectDTO dto = new VulTemplateDetectDTO();
        dto.setTemplateId(template.getTemplateId());
        dto.setYamlId(template.getYamlId());
        dto.setFlow(template.getFlow());
        dto.setVariables(parseJsonMap(template.getVariables()));

        List<VulHttpStep> steps = httpStepMapper.selectList(
                new LambdaQueryWrapper<VulHttpStep>()
                        .eq(VulHttpStep::getDeletedAt, 0L)
                        .eq(VulHttpStep::getTemplateId, templateId)
                        .orderByAsc(VulHttpStep::getStepOrder));
        dto.setHttpSteps(steps.stream().map(step -> {
            VulTemplateDetectDTO.HttpStepDetect sd = new VulTemplateDetectDTO.HttpStepDetect();
            sd.setStepOrder(step.getStepOrder());
            sd.setHttpName(step.getHttpName());
            sd.setMethod(step.getMethod());
            sd.setPath(parseJsonList(step.getPath()));
            sd.setHeaders(parseJsonMapStr(step.getHeaders()));
            sd.setAttack(step.getAttack());
            sd.setMatchersCondition(step.getMatchersCondition());
            sd.setPayloads(parseJsonMap(step.getPayloads()));
            sd.setStopAtFirstMatch(step.getStopAtFirstMatch());
            sd.setSelfContained(step.getSelfContained());
            sd.setRedirects(step.getRedirects());
            sd.setMaxRedirects(step.getMaxRedirects());
            sd.setHostRedirects(step.getHostRedirects());
            sd.setUnsafe(step.getUnsafe());
            sd.setCookieReuse(step.getCookieReuse());
            sd.setReqCondition(step.getReqCondition());
            sd.setMatchers(getMatcherDetects(templateId, step.getStepOrder()));
            sd.setExtractors(getExtractorDetects(templateId, step.getStepOrder()));
            return sd;
        }).toList());
        return dto;
    }

    @Override
    public VulTemplate getByYamlId(String yamlId) {
        return baseMapper.selectOne(
                new LambdaQueryWrapper<VulTemplate>()
                        .eq(VulTemplate::getDeletedAt, 0L)
                        .eq(VulTemplate::getYamlId, yamlId));
    }

    // ── 导入：标签 ────────────────────────────────────

    private void saveTags(Long templateId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) return;
        for (String name : tagNames) {
            String lower = name.toLowerCase();
            VulTag tag = tagMapper.selectOne(
                    new LambdaQueryWrapper<VulTag>()
                            .eq(VulTag::getDeletedAt, 0L)
                            .eq(VulTag::getName, lower));
            if (tag == null) {
                tag = new VulTag();
                tag.setName(lower);
                tagMapper.insert(tag);
            }
            VulTemplateTag tt = new VulTemplateTag();
            tt.setTemplateId(templateId);
            tt.setTagId(tag.getTagId());
            templateTagMapper.insert(tt);
        }
    }

    private List<String> parseTagList(Object tagsRaw) {
        if (tagsRaw == null) return Collections.emptyList();
        if (tagsRaw instanceof String s) {
            return Arrays.stream(s.split(","))
                    .map(String::trim)
                    .filter(s2 -> !s2.isEmpty())
                    .toList();
        }
        if (tagsRaw instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        return Collections.emptyList();
    }

    // ── 导入：参考链接 ────────────────────────────────

    @SuppressWarnings("unchecked")
    private void saveReferences(Long templateId, Object refRaw) {
        if (refRaw == null) return;
        if (refRaw instanceof List<?> list) {
            for (Object item : list) {
                VulReference ref = new VulReference();
                ref.setTemplateId(templateId);
                if (item instanceof String s) {
                    ref.setUrl(s.trim());
                } else if (item instanceof Map<?, ?> m) {
                    ref.setUrl(str(((Map<String, Object>) m).get("url"), str(m.get("link"))));
                    ref.setTitle(str(((Map<String, Object>) m).get("title")));
                }
                if (ref.getUrl() != null && !ref.getUrl().isEmpty()) {
                    referenceMapper.insert(ref);
                }
            }
        } else if (refRaw instanceof String s && !s.isBlank()) {
            VulReference ref = new VulReference();
            ref.setTemplateId(templateId);
            ref.setUrl(s.trim());
            referenceMapper.insert(ref);
        }
    }

    // ── 导入：分类 ────────────────────────────────────

    private void saveCategories(Long templateId, List<Long> categoryIds) {
        for (Long catId : categoryIds) {
            VulTemplateCategory tc = new VulTemplateCategory();
            tc.setTemplateId(templateId);
            tc.setCategoryId(catId);
            templateCategoryMapper.insert(tc);
        }
    }

    // ── 导入：HTTP 步骤 ───────────────────────────────

    @SuppressWarnings("unchecked")
    private void saveHttpSteps(Long templateId, List<?> steps) {
        int order = 1;
        for (Object item : steps) {
            if (!(item instanceof Map<?, ?>)) continue;
            Map<String, Object> s = (Map<String, Object>) item;

            VulHttpStep step = new VulHttpStep();
            step.setTemplateId(templateId);
            step.setStepOrder(order);
            step.setHttpName(str(s.get("name")));
            step.setMethod(str(s.get("method")));
            step.setPath(toJson(pathToList(s.get("path"))));
            step.setHeaders(toJson(s.get("headers")));
            step.setMatchersCondition(str(s.get("matchers-condition"), "or"));
            step.setAttack(str(s.get("attack")));
            step.setPayloads(toJson(s.get("payloads")));
            step.setStopAtFirstMatch(bool(s.get("stop-at-first-match"), false));
            step.setSelfContained(bool(s.get("self-contained"), false));
            step.setRedirects(bool(s.get("redirects"), false));
            step.setMaxRedirects(integer(s.get("max-redirects")));
            step.setHostRedirects(bool(s.get("host-redirects"), false));
            step.setUnsafe(bool(s.get("unsafe"), false));
            step.setCookieReuse(bool(s.get("cookie-reuse"), false));
            step.setReqCondition(bool(s.get("req-condition"), false));

            // body → vul_text_content
            String body = str(s.get("body"));
            if (!body.isEmpty()) {
                VulTextContent tc = new VulTextContent();
                tc.setContent(body);
                textContentMapper.insert(tc);
                step.setBodyTextId(tc.getTextId());
            }

            // raw → vul_text_content
            Object rawObj = s.get("raw");
            String rawText = rawToList(rawObj);
            if (!rawText.isEmpty()) {
                VulTextContent tc = new VulTextContent();
                tc.setContent(rawText);
                textContentMapper.insert(tc);
                step.setRawTextId(tc.getTextId());
            }

            httpStepMapper.insert(step);

            // matchers
            Object matchersObj = s.get("matchers");
            if (matchersObj instanceof List<?> matchers) {
                saveMatchers(templateId, order, matchers);
            }

            // extractors
            Object extractorsObj = s.get("extractors");
            if (extractorsObj instanceof List<?> extractors) {
                saveExtractors(templateId, order, extractors);
            }

            order++;
        }
    }

    @SuppressWarnings("unchecked")
    private void saveMatchers(Long templateId, int stepOrder, List<?> matchers) {
        for (Object obj : matchers) {
            if (!(obj instanceof Map<?, ?>)) continue;
            Map<String, Object> m = (Map<String, Object>) obj;

            VulMatcher matcher = new VulMatcher();
            matcher.setTemplateId(templateId);
            matcher.setStepOrder(stepOrder);
            matcher.setType(str(m.get("type")));
            matcher.setPart(str(m.get("part")));
            matcher.setInnerCondition(str(m.get("condition"), "or"));
            matcher.setNegative(bool(m.get("negative"), false));
            matcher.setCaseInsensitive(bool(m.get("case-insensitive"), false));
            matcher.setInternal(bool(m.get("internal"), false));
            matcher.setMatchAll(bool(m.get("match-all"), false));
            matcher.setMatcherName(str(m.get("name")));
            matcher.setConfig(toJson(extractConfig(m, MATCHER_CONFIG_KEYS)));
            matcherMapper.insert(matcher);
        }
    }

    @SuppressWarnings("unchecked")
    private void saveExtractors(Long templateId, int stepOrder, List<?> extractors) {
        for (Object obj : extractors) {
            if (!(obj instanceof Map<?, ?>)) continue;
            Map<String, Object> e = (Map<String, Object>) obj;

            VulExtractor extr = new VulExtractor();
            extr.setTemplateId(templateId);
            extr.setStepOrder(stepOrder);
            extr.setType(str(e.get("type")));
            extr.setPart(str(e.get("part")));
            extr.setExtractorName(str(e.get("name")));
            extr.setConfig(toJson(extractConfig(e, EXTRACTOR_CONFIG_KEYS)));
            extr.setInternal(bool(e.get("internal"), false));
            extr.setGroupNum(integer(e.get("group"), 1));

            // nuclei extractor: "group" → "groupNum"
            if (e.containsKey("group") && !e.containsKey("groupNum")) {
                extr.setGroupNum(integer(e.get("group"), 1));
            }

            extractorMapper.insert(extr);
        }
    }

    // ── 软删除级联 ────────────────────────────────────

    private void softDeleteByTemplateId(Long templateId, long nowMs) {
        templateTagMapper.update(null,
                new LambdaUpdateWrapper<VulTemplateTag>()
                        .eq(VulTemplateTag::getTemplateId, templateId)
                        .eq(VulTemplateTag::getDeletedAt, 0L)
                        .set(VulTemplateTag::getDeletedAt, nowMs));
        referenceMapper.update(null,
                new LambdaUpdateWrapper<VulReference>()
                        .eq(VulReference::getTemplateId, templateId)
                        .eq(VulReference::getDeletedAt, 0L)
                        .set(VulReference::getDeletedAt, nowMs));
        matcherMapper.update(null,
                new LambdaUpdateWrapper<VulMatcher>()
                        .eq(VulMatcher::getTemplateId, templateId)
                        .eq(VulMatcher::getDeletedAt, 0L)
                        .set(VulMatcher::getDeletedAt, nowMs));
        extractorMapper.update(null,
                new LambdaUpdateWrapper<VulExtractor>()
                        .eq(VulExtractor::getTemplateId, templateId)
                        .eq(VulExtractor::getDeletedAt, 0L)
                        .set(VulExtractor::getDeletedAt, nowMs));
        httpStepMapper.update(null,
                new LambdaUpdateWrapper<VulHttpStep>()
                        .eq(VulHttpStep::getTemplateId, templateId)
                        .eq(VulHttpStep::getDeletedAt, 0L)
                        .set(VulHttpStep::getDeletedAt, nowMs));
        templateCategoryMapper.update(null,
                new LambdaUpdateWrapper<VulTemplateCategory>()
                        .eq(VulTemplateCategory::getTemplateId, templateId)
                        .eq(VulTemplateCategory::getDeletedAt, 0L)
                        .set(VulTemplateCategory::getDeletedAt, nowMs));
    }

    // ── 详情组装 ─────────────────────────────────────

    private VulTemplateVO.Response assembleDetail(VulTemplate template) {
        Long templateId = template.getTemplateId();
        VulTemplateVO.Response vo = templateMapstruct.toResponseVO(template);
        vo.setVariables(parseJsonMap(template.getVariables()));
        vo.setMetadata(parseJsonMap(template.getMetadata()));
        vo.setTags(getTagNames(templateId));
        vo.setReferences(getReferenceVOs(templateId));
        vo.setCategories(getCategoryBriefVOs(templateId));
        vo.setHttpSteps(getHttpStepVOs(templateId));
        return vo;
    }

    private List<String> getTagNames(Long templateId) {
        List<Long> tagIds = templateTagMapper.selectList(
                new LambdaQueryWrapper<VulTemplateTag>()
                        .eq(VulTemplateTag::getDeletedAt, 0L)
                        .eq(VulTemplateTag::getTemplateId, templateId)
        ).stream().map(VulTemplateTag::getTagId).toList();
        if (tagIds.isEmpty()) return Collections.emptyList();
        return tagMapper.selectList(
                new LambdaQueryWrapper<VulTag>()
                        .eq(VulTag::getDeletedAt, 0L)
                        .in(VulTag::getTagId, tagIds)
        ).stream().map(VulTag::getName).toList();
    }

    private List<String> getCategoryNames(Long templateId) {
        return getCategoryBriefVOs(templateId).stream()
                .map(VulTemplateVO.Response.CategoryBriefVO::getName).toList();
    }

    private List<VulTemplateVO.Response.CategoryBriefVO> getCategoryBriefVOs(Long templateId) {
        List<Long> catIds = templateCategoryMapper.selectList(
                new LambdaQueryWrapper<VulTemplateCategory>()
                        .eq(VulTemplateCategory::getDeletedAt, 0L)
                        .eq(VulTemplateCategory::getTemplateId, templateId)
        ).stream().map(VulTemplateCategory::getCategoryId).toList();
        if (catIds.isEmpty()) return Collections.emptyList();
        return categoryMapper.selectList(
                new LambdaQueryWrapper<VulCategory>()
                        .eq(VulCategory::getDeletedAt, 0L)
                        .in(VulCategory::getCategoryId, catIds)
        ).stream().map(c -> {
            VulTemplateVO.Response.CategoryBriefVO cv = new VulTemplateVO.Response.CategoryBriefVO();
            cv.setCategoryId(c.getCategoryId());
            cv.setName(c.getName());
            return cv;
        }).toList();
    }

    private List<VulTemplateVO.Response.ReferenceVO> getReferenceVOs(Long templateId) {
        return referenceMapper.selectList(
                new LambdaQueryWrapper<VulReference>()
                        .eq(VulReference::getDeletedAt, 0L)
                        .eq(VulReference::getTemplateId, templateId)
        ).stream().map(r -> {
            VulTemplateVO.Response.ReferenceVO rv = new VulTemplateVO.Response.ReferenceVO();
            rv.setUrl(r.getUrl());
            rv.setTitle(r.getTitle());
            return rv;
        }).toList();
    }

    private List<VulTemplateVO.Response.HttpStepVO> getHttpStepVOs(Long templateId) {
        return httpStepMapper.selectList(
                new LambdaQueryWrapper<VulHttpStep>()
                        .eq(VulHttpStep::getDeletedAt, 0L)
                        .eq(VulHttpStep::getTemplateId, templateId)
                        .orderByAsc(VulHttpStep::getStepOrder)
        ).stream().map(step -> {
            VulTemplateVO.Response.HttpStepVO sv = new VulTemplateVO.Response.HttpStepVO();
            sv.setHttpId(step.getHttpId());
            sv.setStepOrder(step.getStepOrder());
            sv.setHttpName(step.getHttpName());
            sv.setMethod(step.getMethod());
            sv.setPath(parseJsonList(step.getPath()));
            sv.setHeaders(parseJsonMapStr(step.getHeaders()));
            sv.setAttack(step.getAttack());
            sv.setMatchersCondition(step.getMatchersCondition());
            sv.setPayloads(parseJsonMap(step.getPayloads()));
            sv.setStopAtFirstMatch(step.getStopAtFirstMatch());
            sv.setSelfContained(step.getSelfContained());
            sv.setRedirects(step.getRedirects());
            sv.setMaxRedirects(step.getMaxRedirects());
            sv.setHostRedirects(step.getHostRedirects());
            sv.setUnsafe(step.getUnsafe());
            sv.setCookieReuse(step.getCookieReuse());
            sv.setReqCondition(step.getReqCondition());
            sv.setMatchers(getMatcherVOs(templateId, step.getStepOrder()));
            sv.setExtractors(getExtractorVOs(templateId, step.getStepOrder()));
            return sv;
        }).toList();
    }

    private List<VulTemplateVO.Response.MatcherVO> getMatcherVOs(Long templateId, Integer stepOrder) {
        return matcherMapper.selectList(
                new LambdaQueryWrapper<VulMatcher>()
                        .eq(VulMatcher::getDeletedAt, 0L)
                        .eq(VulMatcher::getTemplateId, templateId)
                        .and(w -> w.eq(VulMatcher::getStepOrder, stepOrder)
                                .or().isNull(VulMatcher::getStepOrder))
        ).stream().map(m -> {
            VulTemplateVO.Response.MatcherVO mv = new VulTemplateVO.Response.MatcherVO();
            mv.setMatcherId(m.getMatcherId());
            mv.setType(m.getType());
            mv.setPart(m.getPart());
            mv.setInnerCondition(m.getInnerCondition());
            mv.setNegative(m.getNegative());
            mv.setCaseInsensitive(m.getCaseInsensitive());
            mv.setInternal(m.getInternal());
            mv.setMatchAll(m.getMatchAll());
            mv.setMatcherName(m.getMatcherName());
            mv.setConfig(parseJsonMap(m.getConfig()));
            return mv;
        }).toList();
    }

    private List<VulTemplateVO.Response.ExtractorVO> getExtractorVOs(Long templateId, Integer stepOrder) {
        return extractorMapper.selectList(
                new LambdaQueryWrapper<VulExtractor>()
                        .eq(VulExtractor::getDeletedAt, 0L)
                        .eq(VulExtractor::getTemplateId, templateId)
                        .and(w -> w.eq(VulExtractor::getStepOrder, stepOrder)
                                .or().isNull(VulExtractor::getStepOrder))
        ).stream().map(e -> {
            VulTemplateVO.Response.ExtractorVO ev = new VulTemplateVO.Response.ExtractorVO();
            ev.setExtractorId(e.getExtractorId());
            ev.setType(e.getType());
            ev.setPart(e.getPart());
            ev.setExtractorName(e.getExtractorName());
            ev.setConfig(parseJsonMap(e.getConfig()));
            ev.setInternal(e.getInternal());
            ev.setGroupNum(e.getGroupNum());
            return ev;
        }).toList();
    }

    private List<VulTemplateDetectDTO.MatcherDetect> getMatcherDetects(Long templateId, Integer stepOrder) {
        return matcherMapper.selectList(
                new LambdaQueryWrapper<VulMatcher>()
                        .eq(VulMatcher::getDeletedAt, 0L)
                        .eq(VulMatcher::getTemplateId, templateId)
                        .and(w -> w.eq(VulMatcher::getStepOrder, stepOrder)
                                .or().isNull(VulMatcher::getStepOrder))
        ).stream().map(m -> {
            VulTemplateDetectDTO.MatcherDetect md = new VulTemplateDetectDTO.MatcherDetect();
            md.setType(m.getType());
            md.setPart(m.getPart());
            md.setInnerCondition(m.getInnerCondition());
            md.setNegative(m.getNegative());
            md.setCaseInsensitive(m.getCaseInsensitive());
            md.setMatchAll(m.getMatchAll());
            md.setConfig(parseJsonMap(m.getConfig()));
            return md;
        }).toList();
    }

    private List<VulTemplateDetectDTO.ExtractorDetect> getExtractorDetects(Long templateId, Integer stepOrder) {
        return extractorMapper.selectList(
                new LambdaQueryWrapper<VulExtractor>()
                        .eq(VulExtractor::getDeletedAt, 0L)
                        .eq(VulExtractor::getTemplateId, templateId)
                        .and(w -> w.eq(VulExtractor::getStepOrder, stepOrder)
                                .or().isNull(VulExtractor::getStepOrder))
        ).stream().map(e -> {
            VulTemplateDetectDTO.ExtractorDetect ed = new VulTemplateDetectDTO.ExtractorDetect();
            ed.setType(e.getType());
            ed.setPart(e.getPart());
            ed.setExtractorName(e.getExtractorName());
            ed.setConfig(parseJsonMap(e.getConfig()));
            ed.setInternal(e.getInternal());
            ed.setGroupNum(e.getGroupNum());
            return ed;
        }).toList();
    }

    // ── 标签过滤 ─────────────────────────────────────

    private Set<Long> getTemplateIdsByTag(String tagName) {
        List<VulTag> tags = tagMapper.selectList(
                new LambdaQueryWrapper<VulTag>()
                        .eq(VulTag::getDeletedAt, 0L)
                        .eq(VulTag::getName, tagName.toLowerCase()));
        if (tags.isEmpty()) return Collections.emptySet();
        return templateTagMapper.selectList(
                new LambdaQueryWrapper<VulTemplateTag>()
                        .eq(VulTemplateTag::getDeletedAt, 0L)
                        .eq(VulTemplateTag::getTagId, tags.get(0).getTagId())
        ).stream().map(VulTemplateTag::getTemplateId).collect(Collectors.toSet());
    }

    // ── JSON 解析工具 ────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(String json) {
        if (StrUtil.isBlank(json)) return null;
        try { return JSONUtil.toBean(json, Map.class); } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseJsonMapStr(String json) {
        if (StrUtil.isBlank(json)) return null;
        try { return JSONUtil.toBean(json, Map.class); } catch (Exception e) { return null; }
    }

    @SuppressWarnings("unchecked")
    private List<String> parseJsonList(String json) {
        if (StrUtil.isBlank(json)) return null;
        try { return JSONUtil.toList(json, String.class); } catch (Exception e) { return null; }
    }

    // ── 导入解析工具 ──────────────────────────────────

    private String normalizeSeverity(String raw) {
        if (raw == null || raw.isEmpty()) return "unknown";
        String s = raw.trim().toLowerCase();
        if ("informational".equals(s) || "none".equals(s)) return "info";
        return VALID_SEVERITIES.contains(s) ? s : "unknown";
    }

    /**
     * 从匹配器/提取器的 map 中提取 type-specific 配置字段。
     * 如 matcher {type:"word", words:[...]} → config {"words":[...]}
     */
    private Map<String, Object> extractConfig(Map<String, Object> source, Set<String> keys) {
        Map<String, Object> config = new LinkedHashMap<>();
        for (String key : keys) {
            Object val = source.get(key);
            if (val != null) {
                config.put(key, val);
            }
        }
        return config.isEmpty() ? null : config;
    }

    /** raw 字段可能是 List<String>（多行）或单个 String */
    private String rawToList(Object raw) {
        if (raw == null) return "";
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining("\n"))
                    .trim();
        }
        return raw.toString().trim();
    }

    /** path 字段可能是 String（单路径）或 List<String>（多路径） */
    private List<String> pathToList(Object path) {
        if (path == null) return null;
        if (path instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of(path.toString());
    }

    // ── 安全类型转换 ──────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object obj) {
        if (obj instanceof Map<?, ?>) return (Map<String, Object>) obj;
        return Collections.emptyMap();
    }

    private String str(Object obj) { return str(obj, ""); }

    private String str(Object obj, String defaultVal) {
        if (obj == null) return defaultVal;
        String s = obj.toString().trim();
        return s.isEmpty() ? defaultVal : s;
    }

    private Double dbl(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(obj.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Boolean bool(Object obj, boolean defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Boolean b) return b;
        return defaultVal;
    }

    private Integer integer(Object obj) { return integer(obj, null); }

    private Integer integer(Object obj, Integer defaultVal) {
        if (obj == null) return defaultVal;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (NumberFormatException e) { return defaultVal; }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s && s.isBlank()) return null;
        return JSONUtil.toJsonStr(obj);
    }
}
