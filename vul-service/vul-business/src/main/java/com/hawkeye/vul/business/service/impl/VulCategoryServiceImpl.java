package com.hawkeye.vul.business.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.vul.business.mapstruct.VulMapstruct;
import com.hawkeye.vul.business.mapper.VulCategoryMapper;
import com.hawkeye.vul.business.mapper.VulCategoryMappingMapper;
import com.hawkeye.vul.business.service.VulCategoryService;
import com.hawkeye.vul.common.pojo.entity.VulCategory;
import com.hawkeye.vul.common.pojo.entity.VulCategoryMapping;
import com.hawkeye.vul.common.pojo.vo.category.VulCategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VulCategoryServiceImpl extends ServiceImpl<VulCategoryMapper, VulCategory>
        implements VulCategoryService {

    private final VulMapstruct vulMapstruct;
    private final VulCategoryMappingMapper vulCategoryMappingMapper;

    @Override
    public List<VulCategoryVO.Response> listCategories(Long parentId, String name) {
        LambdaQueryWrapper<VulCategory> wrapper = new LambdaQueryWrapper<VulCategory>()
                .eq(parentId != null, VulCategory::getParentId, parentId)
                .like(StrUtil.isNotBlank(name), VulCategory::getName, name)
                .orderByAsc(VulCategory::getName);

        return baseMapper.selectList(wrapper).stream()
                .map(vulMapstruct::toCategoryVO)
                .toList();
    }

    @Override
    public VulCategoryVO.Response create(VulCategoryVO.Request request) {
        VulCategory category = vulMapstruct.toCategoryEntity(request);
        baseMapper.insert(category);
        return vulMapstruct.toCategoryVO(category);
    }

    @Override
    public VulCategoryVO.Response update(Long categoryId, VulCategoryVO.Request request) {
        VulCategory category = baseMapper.selectById(categoryId);
        if (category == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "分类不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        if (request.getParentId() != null && request.getParentId().equals(categoryId)) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(),
                    "父分类不能是自己", HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        if (request.getParentId() != null && isAncestor(categoryId, request.getParentId())) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(),
                    "不能将分类挂到自己的子分类下，会形成循环",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getParentId() != null) {
            category.setParentId(request.getParentId());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        baseMapper.updateById(category);

        VulCategory updated = baseMapper.selectById(categoryId);
        return vulMapstruct.toCategoryVO(updated);
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        VulCategory category = baseMapper.selectById(categoryId);
        if (category == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "分类不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        long childCount = baseMapper.selectCount(
                new LambdaQueryWrapper<VulCategory>()
                        .eq(VulCategory::getParentId, categoryId));
        if (childCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在子分类，请先删除子分类",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        long mappingCount = vulCategoryMappingMapper.selectCount(
                new LambdaQueryWrapper<VulCategoryMapping>()
                        .eq(VulCategoryMapping::getCategoryId, categoryId));
        if (mappingCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在关联模板，请先移除所有模板",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        baseMapper.deleteById(categoryId);
    }

    @Override
    @Transactional
    public int addTemplates(Long categoryId, List<Long> templateIds) {
        if (baseMapper.selectById(categoryId) == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(),
                    "分类不存在", HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        Set<Long> existing = new HashSet<>(vulCategoryMappingMapper.selectList(
                new LambdaQueryWrapper<VulCategoryMapping>()
                        .eq(VulCategoryMapping::getCategoryId, categoryId)
                        .in(VulCategoryMapping::getTemplateId, templateIds)
        ).stream().map(VulCategoryMapping::getTemplateId).toList());

        List<VulCategoryMapping> newMappings = new ArrayList<>();
        for (Long templateId : templateIds) {
            if (!existing.contains(templateId)) {
                VulCategoryMapping mapping = new VulCategoryMapping();
                mapping.setCategoryId(categoryId);
                mapping.setTemplateId(templateId);
                newMappings.add(mapping);
            }
        }

        if (!newMappings.isEmpty()) {
            for (VulCategoryMapping mapping : newMappings) {
                vulCategoryMappingMapper.insert(mapping);
            }
        }
        return newMappings.size();
    }

    @Override
    @Transactional
    public int removeTemplates(Long categoryId, List<Long> templateIds) {
        if (templateIds == null || templateIds.isEmpty()) {
            return 0;
        }
        return vulCategoryMappingMapper.delete(
                new LambdaQueryWrapper<VulCategoryMapping>()
                        .eq(VulCategoryMapping::getCategoryId, categoryId)
                        .in(VulCategoryMapping::getTemplateId, templateIds));
    }

    private boolean isAncestor(Long categoryId, Long targetId) {
        Set<Long> visited = new HashSet<>();
        Long current = targetId;
        while (current != null && !visited.contains(current)) {
            if (current.equals(categoryId)) {
                return true;
            }
            visited.add(current);
            VulCategory parent = baseMapper.selectById(current);
            if (parent == null) {
                break;
            }
            current = parent.getParentId();
        }
        return false;
    }
}
