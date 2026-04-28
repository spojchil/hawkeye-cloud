package com.hawkeye.asset.business.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hawkeye.asset.business.mapstruct.AssetCategoryMapstruct;
import com.hawkeye.asset.business.mapper.AssetCategoryMapper;
import com.hawkeye.asset.business.mapper.AssetCategoryMappingMapper;
import com.hawkeye.asset.business.service.AssetCategoryService;
import com.hawkeye.asset.common.pojo.entity.AssetCategory;
import com.hawkeye.asset.common.pojo.entity.AssetCategoryMapping;
import com.hawkeye.asset.common.pojo.vo.category.CategoryVO;
import com.common.utils.response.ApiException;
import com.common.utils.response.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 资产分类服务实现
 * <p>
 * 负责分类的 CRUD 以及分类与资产的关联管理（多对多关系）。
 * 分类支持树形结构：每个分类可以有父分类（parentId），null 表示顶级分类。
 * <p>
 * <b>关键业务规则：</b>
 * <ul>
 *   <li><b>创建时确定层级：</b> 创建分类时指定 parentId，创建后不允许修改父分类</li>
 *   <li><b>删除保护：</b> 存在子分类或已关联资产的分类不允许删除</li>
 *   <li><b>去重：</b> 向分类中添加资产时，已存在的关联会被跳过，只统计新增数量</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetCategoryServiceImpl extends ServiceImpl<AssetCategoryMapper, AssetCategory>
        implements AssetCategoryService {

    private final AssetCategoryMapstruct categoryMapstruct;
    private final AssetCategoryMappingMapper mappingMapper;

    @Override
    public List<CategoryVO.Response> listCategories(Long parentId, String name) {
        /*
         * 查询条件：如果指定了 parentId 则精确匹配，否则查顶级分类（parentId IS NULL）。
         * 使用 LambdaQueryWrapper 避免字段名硬编码，同时享受 IDE 的类型安全校验。
         */
        LambdaQueryWrapper<AssetCategory> wrapper = new LambdaQueryWrapper<AssetCategory>()
                .eq(parentId != null, AssetCategory::getParentId, parentId)
                .isNull(parentId == null, AssetCategory::getParentId)
                .like(StrUtil.isNotBlank(name), AssetCategory::getName, name)
                .orderByAsc(AssetCategory::getName);
        return baseMapper.selectList(wrapper)
                .stream()
                .map(categoryMapstruct::toResponseVO)
                .toList();
    }

    @Override
    public CategoryVO.Response create(CategoryVO.Request request) {
        AssetCategory category = categoryMapstruct.toEntity(request);
        baseMapper.insert(category);
        return categoryMapstruct.toResponseVO(category);
    }

    /**
     * 部分更新分类的普通字段（名称、描述）。
     * <p>
     * 不允许修改父分类（parentId），传入的 request.parentId 会被忽略。
     * 如果 categoryId 不存在则抛异常。
     */
    @Override
    public CategoryVO.Response update(Long categoryId, CategoryVO.Request request) {
        LambdaUpdateWrapper<AssetCategory> wrapper = new LambdaUpdateWrapper<AssetCategory>()
                .eq(AssetCategory::getCategoryId, categoryId)
                .set(request.getName() != null, AssetCategory::getName, request.getName())
                .set(request.getDescription() != null, AssetCategory::getDescription, request.getDescription());

        if (baseMapper.update(null, wrapper) == 0) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "分类不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        AssetCategory updated = baseMapper.selectById(categoryId);
        return categoryMapstruct.toResponseVO(updated);
    }

    @Override
    @Transactional
    public void delete(Long categoryId) {
        AssetCategory category = baseMapper.selectById(categoryId);
        if (category == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "分类不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        // 检查 1：是否存在子分类——存在则不允许删除，避免产生孤立节点
        long childCount = lambdaQuery().eq(AssetCategory::getParentId, categoryId).count();
        if (childCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在子分类，无法删除",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        // 检查 2：是否存在资产关联——有关联则不允许删除，避免产生悬空引用
        long mappingCount = mappingMapper.selectCount(
                new LambdaQueryWrapper<AssetCategoryMapping>()
                        .eq(AssetCategoryMapping::getCategoryId, categoryId));
        if (mappingCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该分类下存在资产关联，无法删除",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        baseMapper.deleteById(categoryId);
    }

    /**
     * 批量向分类添加资产（去重）。
     * <p>
     * 先用一次 IN 查询查出已存在的关联，避免逐条 SELECT COUNT 的 N+1 问题，
     * 然后仅插入不存在的关联。
     * <p>
     * ★ 加 @Transactional：虽然 selectList + loop insert 在默认 READ_COMMITTED 下仍有并发去重漏洞，
     * 但数据库层有唯一索引 uk_asset_category (asset_id, category_id) 兜底，
     * 并发冲突时 MySQL 抛 DuplicateKeyException 事务回滚，不会产生脏数据。
     */
    @Override
    @Transactional
    public int addAssets(Long categoryId, List<Long> assetIds) {
        if (baseMapper.selectById(categoryId) == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "分类不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        // 1. 一次 IN 查询查出所有已存在的 asset_id，避免 N+1
        List<Long> existingAssetIds = mappingMapper.selectList(
                new LambdaQueryWrapper<AssetCategoryMapping>()
                        .eq(AssetCategoryMapping::getCategoryId, categoryId)
                        .in(AssetCategoryMapping::getAssetId, assetIds)
                        .select(AssetCategoryMapping::getAssetId)
        ).stream().map(AssetCategoryMapping::getAssetId).toList();

        // 2. 过滤 + 去重，只保留新的 assetId
        List<Long> newAssetIds = assetIds.stream()
                .filter(id -> !existingAssetIds.contains(id))
                .distinct()
                .toList();

        // 3. 逐个插入（须通过 MyBatis-Plus 的 insert 方法以触发自动填充：tenant_id、create_time 等）
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

    @Override
    public int removeAssets(Long categoryId, List<Long> assetIds) {
        return mappingMapper.delete(
                new LambdaQueryWrapper<AssetCategoryMapping>()
                        .eq(AssetCategoryMapping::getCategoryId, categoryId)
                        .in(AssetCategoryMapping::getAssetId, assetIds));
    }

}
