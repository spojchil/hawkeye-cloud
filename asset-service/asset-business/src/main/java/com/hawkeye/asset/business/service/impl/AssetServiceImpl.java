package com.hawkeye.asset.business.service.impl;

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
import com.hawkeye.asset.business.mapper.AssetCategoryMappingMapper;
import com.hawkeye.asset.business.mapper.AssetMapper;
import com.hawkeye.asset.business.mapstruct.AssetMapstruct;
import com.hawkeye.asset.business.service.AssetService;
import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import com.hawkeye.asset.common.pojo.entity.Asset;
import com.hawkeye.asset.common.pojo.entity.AssetCategoryMapping;
import com.hawkeye.asset.common.pojo.vo.asset.AssetVO;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 资产服务实现
 */
@Service
@RequiredArgsConstructor
public class AssetServiceImpl extends ServiceImpl<AssetMapper, Asset> implements AssetService {

    private static final int PAGE_SIZE_MAX = 100;

    private final AssetMapstruct assetMapstruct;
    private final AssetCategoryMappingMapper mappingMapper;

    @LogExecutionTime("资产分页查询")
    @Override
    public ListResult<PageAssetVO.Response> pageQuery(PageAssetVO.Request request) {
        int pageSize = Math.min(request.getPageSize() != null ? request.getPageSize() : 10, PAGE_SIZE_MAX);
        int pageNum = request.getPage() != null ? request.getPage() : 1;

        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<Asset>()
                .eq(Asset::getDeletedAt, 0L)
                .like(StrUtil.isNotBlank(request.getName()), Asset::getName, request.getName())
                .like(StrUtil.isNotBlank(request.getRequestHost()), Asset::getRequestHost, request.getRequestHost())
                .eq(request.getRiskLevel() != null, Asset::getRiskLevel, request.getRiskLevel())
                .eq(request.getStatus() != null, Asset::getStatus, request.getStatus())
                .orderByDesc(Asset::getName);

        if (request.getCategoryId() != null) {
            List<Long> assetIds = mappingMapper.selectList(
                    new LambdaQueryWrapper<AssetCategoryMapping>()
                            .eq(AssetCategoryMapping::getDeletedAt, 0L)
                            .eq(AssetCategoryMapping::getCategoryId, request.getCategoryId())
                            .select(AssetCategoryMapping::getAssetId)
            ).stream().map(AssetCategoryMapping::getAssetId).distinct().toList();

            if (assetIds.isEmpty()) {
                return ListResult.result(0, List.of());
            }
            wrapper.in(Asset::getAssetId, assetIds);
        }

        Page<Asset> page = new Page<>(pageNum, pageSize);
        IPage<Asset> result = baseMapper.selectPage(page, wrapper);

        List<PageAssetVO.Response> voList = result.getRecords()
                .stream()
                .map(assetMapstruct::toListAssetVO)
                .toList();

        return ListResult.result((int) result.getTotal(), voList);
    }

    @LogExecutionTime("查询资产详情")
    @Override
    public AssetVO.Response getById(Long assetId) {
        Asset asset = baseMapper.selectOne(
                new LambdaQueryWrapper<Asset>()
                        .eq(Asset::getAssetId, assetId)
                        .eq(Asset::getDeletedAt, 0L));
        if (asset == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "资产不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }
        return assetMapstruct.toResponseVO(asset);
    }

    @LogExecutionTime("创建资产")
    @Override
    @Transactional
    public AssetVO.Response create(AssetVO.Request request) {
        Asset asset = assetMapstruct.toEntity(request);

        if (asset.getStatus() == null) {
            asset.setStatus(AssetStatusEnum.ENABLED);
        }
        if (asset.getRiskLevel() == null) {
            asset.setRiskLevel(AssetRiskEnum.UNKNOWN);
        }

        baseMapper.insert(asset);
        return assetMapstruct.toResponseVO(asset);
    }

    @LogExecutionTime("更新资产")
    @Override
    @Transactional
    public AssetVO.Response update(Long assetId, AssetVO.Request request) {
        if (request.getName() == null && request.getRequestProtocol() == null
                && request.getRequestHost() == null && request.getRequestPort() == null
                && request.getRequestPath() == null && request.getDescription() == null
                && request.getStatus() == null && request.getRiskLevel() == null) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "至少需要提供一个更新字段",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }
        LambdaUpdateWrapper<Asset> wrapper = new LambdaUpdateWrapper<Asset>()
                .eq(Asset::getAssetId, assetId)
                .eq(Asset::getDeletedAt, 0L)
                .set(request.getName() != null, Asset::getName, request.getName())
                .set(request.getRequestProtocol() != null, Asset::getRequestProtocol, request.getRequestProtocol())
                .set(request.getRequestHost() != null, Asset::getRequestHost, request.getRequestHost())
                .set(request.getRequestPort() != null, Asset::getRequestPort, request.getRequestPort())
                .set(request.getRequestPath() != null, Asset::getRequestPath, request.getRequestPath())
                .set(request.getDescription() != null, Asset::getDescription, request.getDescription())
                .set(request.getStatus() != null, Asset::getStatus, request.getStatus())
                .set(request.getRiskLevel() != null, Asset::getRiskLevel, request.getRiskLevel());

        if (baseMapper.update(null, wrapper) == 0) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "资产不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        Asset updated = baseMapper.selectOne(
                new LambdaQueryWrapper<Asset>()
                        .eq(Asset::getAssetId, assetId)
                        .eq(Asset::getDeletedAt, 0L));
        return assetMapstruct.toResponseVO(updated);
    }

    @LogExecutionTime("删除资产")
    @Override
    @Transactional
    public void delete(Long assetId) {
        Asset asset = baseMapper.selectOne(
                new LambdaQueryWrapper<Asset>()
                        .eq(Asset::getAssetId, assetId)
                        .eq(Asset::getDeletedAt, 0L));
        if (asset == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "资产不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        long mappingCount = mappingMapper.selectCount(
                new LambdaQueryWrapper<AssetCategoryMapping>()
                        .eq(AssetCategoryMapping::getDeletedAt, 0L)
                        .eq(AssetCategoryMapping::getAssetId, assetId));
        if (mappingCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该资产存在分类关联，请先移除所有关联再删除",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        long now = System.currentTimeMillis();
        baseMapper.update(null,
                new LambdaUpdateWrapper<Asset>()
                        .eq(Asset::getAssetId, assetId)
                        .set(Asset::getDeletedAt, now));
    }
}
