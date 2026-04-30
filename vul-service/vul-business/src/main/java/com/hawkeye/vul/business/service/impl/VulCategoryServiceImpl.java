package com.hawkeye.vul.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.vul.business.mapper.VulCategoryMapper;
import com.hawkeye.vul.business.mapper.VulTemplateCategoryMapper;
import com.hawkeye.vul.business.service.VulCategoryService;
import com.hawkeye.vul.common.pojo.entity.VulCategory;
import com.hawkeye.vul.common.pojo.entity.VulTemplateCategory;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryVO;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VulCategoryServiceImpl extends ServiceImpl<VulCategoryMapper, VulCategory>
        implements VulCategoryService {

    private final VulTemplateCategoryMapper templateCategoryMapper;

    @Override
    @LogExecutionTime
    public List<VulCategoryVO> tree(Long parentId) {
        LambdaQueryWrapper<VulCategory> wrapper = new LambdaQueryWrapper<VulCategory>()
                .eq(parentId != null, VulCategory::getParentId, parentId)
                .isNull(parentId == null, VulCategory::getParentId)
                .orderByAsc(VulCategory::getSortOrder)
                .orderByAsc(VulCategory::getName);

        List<VulCategory> categories = baseMapper.selectList(wrapper);
        return categories.stream().map(c -> {
            VulCategoryVO vo = toVO(c);
            vo.setChildren(buildChildren(c.getCategoryId()));
            return vo;
        }).toList();
    }

    private List<VulCategoryVO> buildChildren(Long parentId) {
        List<VulCategory> children = baseMapper.selectList(
                new LambdaQueryWrapper<VulCategory>()
                        .eq(VulCategory::getParentId, parentId)
                        .orderByAsc(VulCategory::getSortOrder)
                        .orderByAsc(VulCategory::getName));
        return children.stream().map(c -> {
            VulCategoryVO vo = toVO(c);
            vo.setChildren(buildChildren(c.getCategoryId()));
            return vo;
        }).toList();
    }

    @Override
    @LogExecutionTime
    @Transactional
    public Long create(VulCategoryRequestVO request) {
        VulCategory category = new VulCategory();
        category.setName(request.getName());
        category.setParentId(request.getParentId());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setDescription(request.getDescription());
        baseMapper.insert(category);
        return category.getCategoryId();
    }

    @Override
    @LogExecutionTime
    @Transactional
    public void update(Long categoryId, VulCategoryRequestVO request) {
        VulCategory category = baseMapper.selectById(categoryId);
        if (category == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "分类不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
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
                .set(request.getName() != null, VulCategory::getName, request.getName())
                .set(request.getParentId() != null, VulCategory::getParentId, request.getParentId())
                .set(request.getSortOrder() != null, VulCategory::getSortOrder, request.getSortOrder())
                .set(request.getDescription() != null, VulCategory::getDescription, request.getDescription());
        baseMapper.update(null, wrapper);
    }

    @Override
    @LogExecutionTime
    @Transactional
    public void delete(Long categoryId) {
        if (baseMapper.selectById(categoryId) == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "分类不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        if (baseMapper.selectCount(
                new LambdaQueryWrapper<VulCategory>().eq(VulCategory::getParentId, categoryId)) > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在子分类，请先删除子分类",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }
        if (templateCategoryMapper.selectCount(
                new LambdaQueryWrapper<VulTemplateCategory>().eq(VulTemplateCategory::getCategoryId, categoryId)) > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在关联模板，请先移除所有模板",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }
        baseMapper.deleteById(categoryId);
    }

    @Override
    @LogExecutionTime
    @Transactional
    public int addTemplates(Long categoryId, List<Long> templateIds) {
        if (baseMapper.selectById(categoryId) == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "分类不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        Set<Long> existing = templateCategoryMapper.selectList(
                new LambdaQueryWrapper<VulTemplateCategory>()
                        .eq(VulTemplateCategory::getCategoryId, categoryId)
                        .in(VulTemplateCategory::getTemplateId, templateIds)
        ).stream().map(VulTemplateCategory::getTemplateId).collect(Collectors.toSet());

        List<VulTemplateCategory> newMappings = new ArrayList<>();
        Set<Long> seen = new HashSet<>(existing);
        for (Long tid : templateIds) {
            if (seen.add(tid)) {
                VulTemplateCategory tc = new VulTemplateCategory();
                tc.setCategoryId(categoryId);
                tc.setTemplateId(tid);
                newMappings.add(tc);
            }
        }

        if (!newMappings.isEmpty()) {
            for (VulTemplateCategory tc : newMappings) {
                templateCategoryMapper.insert(tc);
            }
        }
        return newMappings.size();
    }

    @Override
    @LogExecutionTime
    @Transactional
    public int removeTemplates(Long categoryId, List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) return 0;
        return templateCategoryMapper.delete(
                new LambdaQueryWrapper<VulTemplateCategory>()
                        .eq(VulTemplateCategory::getCategoryId, categoryId)
                        .in(VulTemplateCategory::getTemplateId, templateIds));
    }

    private VulCategoryVO toVO(VulCategory c) {
        VulCategoryVO vo = new VulCategoryVO();
        vo.setCategoryId(c.getCategoryId());
        vo.setName(c.getName());
        vo.setParentId(c.getParentId());
        vo.setSortOrder(c.getSortOrder());
        vo.setDescription(c.getDescription());
        vo.setTemplateCount(templateCategoryMapper.selectCount(
                new LambdaQueryWrapper<VulTemplateCategory>()
                        .eq(VulTemplateCategory::getCategoryId, c.getCategoryId())));
        return vo;
    }

    private boolean isAncestor(Long categoryId, Long targetId) {
        Set<Long> visited = new HashSet<>();
        Long current = targetId;
        while (current != null && visited.add(current)) {
            if (current.equals(categoryId)) return true;
            VulCategory parent = baseMapper.selectById(current);
            if (parent == null) break;
            current = parent.getParentId();
        }
        return false;
    }
}
