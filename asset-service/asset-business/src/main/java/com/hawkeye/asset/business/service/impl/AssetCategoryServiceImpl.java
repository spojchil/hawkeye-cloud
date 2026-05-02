package com.hawkeye.asset.business.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.annotation.LogExecutionTime;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import com.hawkeye.asset.business.mapstruct.AssetCategoryMapstruct;
import com.hawkeye.asset.business.mapper.AssetCategoryMapper;
import com.hawkeye.asset.business.mapper.AssetCategoryMappingMapper;
import com.hawkeye.asset.business.service.AssetCategoryService;
import com.hawkeye.asset.common.pojo.entity.AssetCategory;
import com.hawkeye.asset.common.pojo.entity.AssetCategoryMapping;
import com.hawkeye.asset.common.pojo.vo.category.CategoryVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetCategoryServiceImpl extends ServiceImpl<AssetCategoryMapper, AssetCategory>
        implements AssetCategoryService {

    private final AssetCategoryMapstruct categoryMapstruct;
    private final AssetCategoryMappingMapper mappingMapper;

    @LogExecutionTime("查询分类列表")
    @Override
    public List<CategoryVO.Response> listCategories(Long parentId, String name) {
        LambdaQueryWrapper<AssetCategory> wrapper = new LambdaQueryWrapper<AssetCategory>()
                .eq(AssetCategory::getDeletedAt, 0L)
                .eq(parentId != null, AssetCategory::getParentId, parentId)
                .eq(parentId == null, AssetCategory::getParentId, 0L)
                .like(StrUtil.isNotBlank(name), AssetCategory::getName, name)
                .orderByAsc(AssetCategory::getName);
        return baseMapper.selectList(wrapper)
                .stream()
                .map(categoryMapstruct::toResponseVO)
                .toList();
    }

    @LogExecutionTime("创建分类")
    @Override
    @Transactional
    public CategoryVO.Response create(CategoryVO.Request request) {
        AssetCategory category = categoryMapstruct.toEntity(request);
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        baseMapper.insert(category);
        return categoryMapstruct.toResponseVO(category);
    }

    @LogExecutionTime("更新分类")
    @Override
    @Transactional
    public CategoryVO.Response update(Long categoryId, CategoryVO.Request request) {
        if (request.getName() == null && request.getDescription() == null) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "至少需要提供一个更新字段",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }
        LambdaUpdateWrapper<AssetCategory> wrapper = new LambdaUpdateWrapper<AssetCategory>()
                .eq(AssetCategory::getCategoryId, categoryId)
                .eq(AssetCategory::getDeletedAt, 0L)
                .set(request.getName() != null, AssetCategory::getName, request.getName())
                .set(request.getDescription() != null, AssetCategory::getDescription, request.getDescription());

        if (baseMapper.update(null, wrapper) == 0) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "分类不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        AssetCategory updated = baseMapper.selectOne(
                new LambdaQueryWrapper<AssetCategory>()
                        .eq(AssetCategory::getCategoryId, categoryId)
                        .eq(AssetCategory::getDeletedAt, 0L));
        return categoryMapstruct.toResponseVO(updated);
    }

    @LogExecutionTime("删除分类")
    @Override
    @Transactional
    public void delete(Long categoryId) {
        AssetCategory category = baseMapper.selectOne(
                new LambdaQueryWrapper<AssetCategory>()
                        .eq(AssetCategory::getCategoryId, categoryId)
                        .eq(AssetCategory::getDeletedAt, 0L));
        if (category == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "分类不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        long childCount = baseMapper.selectCount(
                new LambdaQueryWrapper<AssetCategory>()
                        .eq(AssetCategory::getDeletedAt, 0L)
                        .eq(AssetCategory::getParentId, categoryId));
        if (childCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在子分类，无法删除",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        long mappingCount = mappingMapper.selectCount(
                new LambdaQueryWrapper<AssetCategoryMapping>()
                        .eq(AssetCategoryMapping::getDeletedAt, 0L)
                        .eq(AssetCategoryMapping::getCategoryId, categoryId));
        if (mappingCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在资产关联，无法删除",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        long now = System.currentTimeMillis();
        baseMapper.update(null,
                new LambdaUpdateWrapper<AssetCategory>()
                        .eq(AssetCategory::getCategoryId, categoryId)
                        .set(AssetCategory::getDeletedAt, now));
    }

    @LogExecutionTime("分类添加资产")
    @Override
    @Transactional
    public int addAssets(Long categoryId, List<Long> assetIds) {
        AssetCategory category = baseMapper.selectOne(
                new LambdaQueryWrapper<AssetCategory>()
                        .eq(AssetCategory::getCategoryId, categoryId)
                        .eq(AssetCategory::getDeletedAt, 0L));
        if (category == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "分类不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        List<Long> existingAssetIds = mappingMapper.selectList(
                new LambdaQueryWrapper<AssetCategoryMapping>()
                        .eq(AssetCategoryMapping::getDeletedAt, 0L)
                        .eq(AssetCategoryMapping::getCategoryId, categoryId)
                        .in(AssetCategoryMapping::getAssetId, assetIds)
                        .select(AssetCategoryMapping::getAssetId)
        ).stream().map(AssetCategoryMapping::getAssetId).toList();

        List<Long> newAssetIds = assetIds.stream()
                .filter(id -> !existingAssetIds.contains(id))
                .distinct()
                .toList();

        int count = 0;
        for (Long assetId : newAssetIds) {
            AssetCategoryMapping mapping = new AssetCategoryMapping();
            mapping.setCategoryId(categoryId);
            mapping.setAssetId(assetId);
            mappingMapper.insert(mapping);
            count++;
        }
        return count;
    }

    @LogExecutionTime("分类移除资产")
    @Override
    @Transactional
    public int removeAssets(Long categoryId, List<Long> assetIds) {
        long now = System.currentTimeMillis();
        int count = 0;
        for (Long assetId : assetIds) {
            count += mappingMapper.update(null,
                    new LambdaUpdateWrapper<AssetCategoryMapping>()
                            .eq(AssetCategoryMapping::getCategoryId, categoryId)
                            .eq(AssetCategoryMapping::getAssetId, assetId)
                            .eq(AssetCategoryMapping::getDeletedAt, 0L)
                            .set(AssetCategoryMapping::getDeletedAt, now));
        }
        return count;
    }

}
