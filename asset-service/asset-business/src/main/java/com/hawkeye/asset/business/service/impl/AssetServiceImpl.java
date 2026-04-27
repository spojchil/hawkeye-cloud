package com.hawkeye.asset.business.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.response.ApiException;
import com.common.utils.response.ListResult;
import com.hawkeye.asset.business.mapstruct.AssetMapstruct;
import com.hawkeye.asset.business.mapper.AssetCategoryMappingMapper;
import com.hawkeye.asset.business.mapper.AssetMapper;
import com.hawkeye.asset.business.service.AssetService;
import com.hawkeye.asset.common.enums.AssetRiskEnum;
import com.hawkeye.asset.common.enums.AssetStatusEnum;
import com.hawkeye.asset.common.pojo.DTO.AssetPageQueryDTO;
import com.hawkeye.asset.common.pojo.entity.Asset;
import com.hawkeye.asset.common.pojo.entity.AssetCategoryMapping;
import com.hawkeye.asset.common.pojo.vo.asset.AssetVO;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 资产服务实现
 * <p>
 * 负责资产的 CRUD，以及按分类筛选分页查询。
 * 资产是核心领域实体，包含协议、域名/IP、端口、路径等请求要素和风险等级、状态等管理属性。
 * <p>
 * <b>关键业务规则：</b>
 * <ul>
 *   <li><b>分页查询：</b> 支持按名称、主机、风险等级、状态筛选，可选关联分类过滤</li>
 *   <li><b>创建默认值：</b> 创建资产时如未指定状态和风险等级，自动设为"启用"和"未知"</li>
 *   <li><b>删除保护：</b> 已关联分类的资产不允许直接删除，需先移除所有分类关联</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AssetServiceImpl extends ServiceImpl<AssetMapper, Asset> implements AssetService {

    private final AssetMapstruct assetMapstruct;
    private final AssetCategoryMappingMapper assetCategoryMappingMapper;

    /**
     * 分页查询资产，支持多维筛选和分类过滤。
     * <p>
     * <b>分类过滤策略（两步查询）：</b>
     * <ol>
     *   <li>先从 {@code asset_category_mapping} 关联表查出指定分类下的所有 asset_id</li>
     *   <li>再用 asset_id IN (...) 作为查询条件去 asset 表分页查询</li>
     * </ol>
     * 这种设计避免了 JOIN 查询，符合项目"不使用联表查询"的设计原则。
     * <p>
     * <b>注意事项：</b>
     * <ul>
     *   <li>如果分类下没有资产（assetIds 为空），直接返回空结果，避免无意义的 IN (NULL) 查询</li>
     *   <li>分类过滤和其他字段过滤（名称、主机等）在同一层 WHERE 中通过 AND 组合</li>
     * </ul>
     */
    @Override
    public ListResult<PageAssetVO.Response> pageQuery(AssetPageQueryDTO dto) {
        // 构建基础查询条件（名称模糊匹配、主机模糊匹配、风险等级精确匹配、状态精确匹配）
        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<Asset>()
                .like(StrUtil.isNotBlank(dto.getName()), Asset::getName, dto.getName())
                .like(StrUtil.isNotBlank(dto.getRequestHost()), Asset::getRequestHost, dto.getRequestHost())
                .eq(dto.getRiskLevel() != null, Asset::getRiskLevel, dto.getRiskLevel())
                .eq(dto.getStatus() != null, Asset::getStatus, dto.getStatus())
                .orderByDesc(Asset::getName);

        // 分类过滤：先查出分类下的资产 ID 列表，再追加 IN 条件
        if (dto.getCategoryId() != null) {
            List<Long> assetIds = assetCategoryMappingMapper.selectList(
                    new LambdaQueryWrapper<AssetCategoryMapping>()
                            .eq(AssetCategoryMapping::getCategoryId, dto.getCategoryId())
                            // 只查 assetId 列，减少数据传输
                            .select(AssetCategoryMapping::getAssetId)
            ).stream().map(AssetCategoryMapping::getAssetId).distinct().toList();

            // 分类下没有资产，直接返回空列表，无需后续分页查询
            if (assetIds.isEmpty()) {
                return ListResult.result(0, Collections.emptyList());
            }
            wrapper.in(Asset::getAssetId, assetIds);
        }

        // MyBatis-Plus 自动分页：自动拼接 COUNT 查询 + LIMIT
        Page<Asset> page = new Page<>(dto.getPage(), dto.getPageSize());
        IPage<Asset> result = baseMapper.selectPage(page, wrapper);

        // Entity → VO 转换（使用 MapStruct 自动映射）
        List<PageAssetVO.Response> voList = result.getRecords()
                .stream()
                .map(assetMapstruct::toListAssetVO)
                .toList();

        // total 是 COUNT 查询的结果，转为 int 以适配 ListResult 接口
        return ListResult.result((int) result.getTotal(), voList);
    }

    @Override
    public AssetVO.Response getById(Long assetId) {
        Asset asset = baseMapper.selectById(assetId);
        if (asset == null) {
            throw new ApiException("资产不存在");
        }
        return assetMapstruct.toResponseVO(asset);
    }

    @Override
    public AssetVO.Response create(AssetVO.Request request) {
        Asset asset = assetMapstruct.toEntity(request);

        // 设置默认值：未指定状态时默认为"启用"，未指定风险等级时默认为"未知"
        if (asset.getStatus() == null) {
            asset.setStatus(AssetStatusEnum.ENABLED);
        }
        if (asset.getRiskLevel() == null) {
            asset.setRiskLevel(AssetRiskEnum.UNKNOWN);
        }

        baseMapper.insert(asset);
        return assetMapstruct.toResponseVO(asset);
    }

    /**
     * 部分更新资产字段。
     * <p>
     * 使用 {@link LambdaUpdateWrapper} 的条件 SET，仅更新 request 中非 null 的字段，
     * null 字段不会覆盖数据库原有值。如果 assetId 不存在（update 影响 0 行）则抛异常。
     */
    @Override
    public AssetVO.Response update(Long assetId, AssetVO.Request request) {
        LambdaUpdateWrapper<Asset> wrapper = new LambdaUpdateWrapper<Asset>()
                .eq(Asset::getAssetId, assetId)
                .set(request.getName() != null, Asset::getName, request.getName())
                .set(request.getRequestMethod() != null, Asset::getRequestMethod, request.getRequestMethod())
                .set(request.getRequestProtocol() != null, Asset::getRequestProtocol, request.getRequestProtocol())
                .set(request.getRequestHost() != null, Asset::getRequestHost, request.getRequestHost())
                .set(request.getRequestPort() != null, Asset::getRequestPort, request.getRequestPort())
                .set(request.getRequestPath() != null, Asset::getRequestPath, request.getRequestPath())
                .set(request.getRequestHeader() != null, Asset::getRequestHeader, request.getRequestHeader())
                .set(request.getDescription() != null, Asset::getDescription, request.getDescription())
                .set(request.getStatus() != null, Asset::getStatus, request.getStatus())
                .set(request.getRiskLevel() != null, Asset::getRiskLevel, request.getRiskLevel());

        if (baseMapper.update(null, wrapper) == 0) {
            throw new ApiException("资产不存在");
        }

        Asset updated = baseMapper.selectById(assetId);
        return assetMapstruct.toResponseVO(updated);
    }

    /**
     * 删除资产。
     * <p>
     * 删除前需要检查该资产是否仍关联了分类，如果关联关系未清理则拒绝删除，
     * 避免在 {@code asset_category_mapping} 表中留下悬空引用。
     */
    @Override
    public void delete(Long assetId) {
        Asset asset = baseMapper.selectById(assetId);
        if (asset == null) {
            throw new ApiException("资产不存在");
        }

        // 检查是否还存在分类关联
        long mappingCount = assetCategoryMappingMapper.selectCount(
                new LambdaQueryWrapper<AssetCategoryMapping>()
                        .eq(AssetCategoryMapping::getAssetId, assetId));
        if (mappingCount > 0) {
            throw new ApiException("该资产存在分类关联，请先移除所有关联再删除");
        }

        baseMapper.deleteById(assetId);
    }
}
