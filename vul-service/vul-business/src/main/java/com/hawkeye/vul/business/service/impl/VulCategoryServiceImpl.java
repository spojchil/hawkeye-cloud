package com.hawkeye.vul.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.vul.business.mapstruct.VulCategoryMapstruct;
import com.hawkeye.vul.business.mapper.VulCategoryMapper;
import com.hawkeye.vul.business.mapper.VulTemplateCategoryMapper;
import com.hawkeye.vul.business.service.VulCategoryService;
import com.hawkeye.vul.common.pojo.entity.VulCategory;
import com.hawkeye.vul.common.pojo.entity.VulTemplateCategory;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class VulCategoryServiceImpl extends ServiceImpl<VulCategoryMapper, VulCategory>
        implements VulCategoryService {

    private final VulCategoryMapstruct categoryMapstruct;
    private final VulTemplateCategoryMapper templateCategoryMapper;

    @Override
    @LogExecutionTime("查询分类树")
    public List<VulCategoryVO.Response> tree(Long parentId) {
        LambdaQueryWrapper<VulCategory> wrapper = new LambdaQueryWrapper<VulCategory>()
                .eq(VulCategory::getDeletedAt, 0L)
                .eq(parentId != null, VulCategory::getParentId, parentId)
                .isNull(parentId == null, VulCategory::getParentId)
                .orderByAsc(VulCategory::getSortOrder)
                .orderByAsc(VulCategory::getName);

        return baseMapper.selectList(wrapper).stream()
                .map(this::toTreeVO)
                .toList();
    }

    private VulCategoryVO.Response toTreeVO(VulCategory c) {
        VulCategoryVO.Response vo = categoryMapstruct.toResponseVO(c);
        vo.setTemplateCount(templateCategoryMapper.selectCount(
                new LambdaQueryWrapper<VulTemplateCategory>()
                        .eq(VulTemplateCategory::getDeletedAt, 0L)
                        .eq(VulTemplateCategory::getCategoryId, c.getCategoryId())));
        vo.setChildren(buildChildren(c.getCategoryId()));
        return vo;
    }

    private List<VulCategoryVO.Response> buildChildren(Long parentId) {
        List<VulCategory> children = baseMapper.selectList(
                new LambdaQueryWrapper<VulCategory>()
                        .eq(VulCategory::getDeletedAt, 0L)
                        .eq(VulCategory::getParentId, parentId)
                        .orderByAsc(VulCategory::getSortOrder)
                        .orderByAsc(VulCategory::getName));
        return children.stream().map(this::toTreeVO).toList();
    }

    @Override
    @LogExecutionTime("创建分类")
    @Transactional
    public VulCategoryVO.Response create(VulCategoryVO.Request request) {
        VulCategory category = categoryMapstruct.toEntity(request);
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        if (category.getSortOrder() == null) {
            category.setSortOrder(0);
        }
        baseMapper.insert(category);
        return categoryMapstruct.toResponseVO(category);
    }

    @Override
    @LogExecutionTime("更新分类")
    @Transactional
    public VulCategoryVO.Response update(Long categoryId, VulCategoryVO.Request request) {
        if (request.getName() == null && request.getDescription() == null
                && request.getParentId() == null && request.getSortOrder() == null) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "至少需要提供一个更新字段",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(categoryId)) {
                throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(),
                        "父分类不能是自己", HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
            }
            if (isAncestor(categoryId, request.getParentId())) {
                throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(),
                        "不能将分类挂到自己的子分类下，会形成循环",
                        HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
            }
        }

        LambdaUpdateWrapper<VulCategory> wrapper = new LambdaUpdateWrapper<VulCategory>()
                .eq(VulCategory::getCategoryId, categoryId)
                .eq(VulCategory::getDeletedAt, 0L)
                .set(request.getName() != null, VulCategory::getName, request.getName())
                .set(request.getDescription() != null, VulCategory::getDescription, request.getDescription())
                .set(request.getParentId() != null, VulCategory::getParentId, request.getParentId())
                .set(request.getSortOrder() != null, VulCategory::getSortOrder, request.getSortOrder());

        if (baseMapper.update(null, wrapper) == 0) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "分类不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        VulCategory updated = baseMapper.selectOne(
                new LambdaQueryWrapper<VulCategory>()
                        .eq(VulCategory::getCategoryId, categoryId)
                        .eq(VulCategory::getDeletedAt, 0L));
        return categoryMapstruct.toResponseVO(updated);
    }

    @Override
    @LogExecutionTime("删除分类")
    @Transactional
    public void delete(Long categoryId) {
        VulCategory category = baseMapper.selectOne(
                new LambdaQueryWrapper<VulCategory>()
                        .eq(VulCategory::getCategoryId, categoryId)
                        .eq(VulCategory::getDeletedAt, 0L));
        if (category == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "分类不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        long childCount = baseMapper.selectCount(
                new LambdaQueryWrapper<VulCategory>()
                        .eq(VulCategory::getDeletedAt, 0L)
                        .eq(VulCategory::getParentId, categoryId));
        if (childCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在子分类，请先删除子分类",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        long mappingCount = templateCategoryMapper.selectCount(
                new LambdaQueryWrapper<VulTemplateCategory>()
                        .eq(VulTemplateCategory::getDeletedAt, 0L)
                        .eq(VulTemplateCategory::getCategoryId, categoryId));
        if (mappingCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在关联模板，请先移除所有模板",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        long now = System.currentTimeMillis();
        baseMapper.update(null,
                new LambdaUpdateWrapper<VulCategory>()
                        .eq(VulCategory::getCategoryId, categoryId)
                        .set(VulCategory::getDeletedAt, now));
    }

    @Override
    @LogExecutionTime("分类添加模板")
    @Transactional
    public int addTemplates(Long categoryId, List<Long> templateIds) {
        VulCategory category = baseMapper.selectOne(
                new LambdaQueryWrapper<VulCategory>()
                        .eq(VulCategory::getCategoryId, categoryId)
                        .eq(VulCategory::getDeletedAt, 0L));
        if (category == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "分类不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        List<Long> existingTemplateIds = templateCategoryMapper.selectList(
                new LambdaQueryWrapper<VulTemplateCategory>()
                        .eq(VulTemplateCategory::getDeletedAt, 0L)
                        .eq(VulTemplateCategory::getCategoryId, categoryId)
                        .in(VulTemplateCategory::getTemplateId, templateIds)
                        .select(VulTemplateCategory::getTemplateId)
        ).stream().map(VulTemplateCategory::getTemplateId).toList();

        List<Long> newTemplateIds = templateIds.stream()
                .filter(id -> !existingTemplateIds.contains(id))
                .distinct()
                .toList();

        int count = 0;
        for (Long templateId : newTemplateIds) {
            VulTemplateCategory mapping = new VulTemplateCategory();
            mapping.setCategoryId(categoryId);
            mapping.setTemplateId(templateId);
            templateCategoryMapper.insert(mapping);
            count++;
        }
        return count;
    }

    @Override
    @LogExecutionTime("分类移除模板")
    @Transactional
    public int removeTemplates(Long categoryId, List<Long> templateIds) {
        long now = System.currentTimeMillis();
        int count = 0;
        for (Long templateId : templateIds) {
            count += templateCategoryMapper.update(null,
                    new LambdaUpdateWrapper<VulTemplateCategory>()
                            .eq(VulTemplateCategory::getCategoryId, categoryId)
                            .eq(VulTemplateCategory::getTemplateId, templateId)
                            .eq(VulTemplateCategory::getDeletedAt, 0L)
                            .set(VulTemplateCategory::getDeletedAt, now));
        }
        return count;
    }

    private boolean isAncestor(Long categoryId, Long targetId) {
        Set<Long> visited = new HashSet<>();
        Long current = targetId;
        while (current != null && visited.add(current)) {
            if (current.equals(categoryId)) return true;
            VulCategory parent = baseMapper.selectOne(
                    new LambdaQueryWrapper<VulCategory>()
                            .eq(VulCategory::getCategoryId, current)
                            .eq(VulCategory::getDeletedAt, 0L));
            if (parent == null) break;
            current = parent.getParentId();
        }
        return false;
    }
}
