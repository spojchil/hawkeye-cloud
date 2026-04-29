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
import com.hawkeye.asset.business.mapstruct.AssetMapstruct;
import com.hawkeye.asset.business.mapper.AssetCategoryMappingMapper;
import com.hawkeye.asset.business.mapper.AssetMapper;
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

    /** 分页查询每页最大条数，防止恶意请求一次性查询过多数据 */
    private static final int PAGE_SIZE_MAX = 100;

    private final AssetMapstruct assetMapstruct;
    private final AssetCategoryMappingMapper mappingMapper;

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
     *   <li>pageSize 上限 {@value #PAGE_SIZE_MAX}，超过自动截断</li>
     * </ul>
     */
    @LogExecutionTime("资产分页查询")
    @Override
    public ListResult<PageAssetVO.Response> pageQuery(PageAssetVO.Request request) {
        // pageSize 上限保护，防止一次查几万条
        int pageSize = Math.min(request.getPageSize() != null ? request.getPageSize() : 10, PAGE_SIZE_MAX);
        int pageNum = request.getPage() != null ? request.getPage() : 1;

        LambdaQueryWrapper<Asset> wrapper = new LambdaQueryWrapper<Asset>()
                .like(StrUtil.isNotBlank(request.getName()), Asset::getName, request.getName())
                .like(StrUtil.isNotBlank(request.getRequestHost()), Asset::getRequestHost, request.getRequestHost())
                .eq(request.getRiskLevel() != null, Asset::getRiskLevel, request.getRiskLevel())
                .eq(request.getStatus() != null, Asset::getStatus, request.getStatus())
                .orderByDesc(Asset::getName);

        // 分类过滤：先查出分类下的资产 ID 列表，再追加 IN 条件
        if (request.getCategoryId() != null) {
            List<Long> assetIds = mappingMapper.selectList(
                    new LambdaQueryWrapper<AssetCategoryMapping>()
                            .eq(AssetCategoryMapping::getCategoryId, request.getCategoryId())
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
        Page<Asset> page = new Page<>(pageNum, pageSize);
        IPage<Asset> result = baseMapper.selectPage(page, wrapper);

        // Entity → VO 转换（使用 MapStruct 自动映射）
        List<PageAssetVO.Response> voList = result.getRecords()
                .stream()
                .map(assetMapstruct::toListAssetVO)
                .toList();

        // total 是 COUNT 查询的结果，转为 int 以适配 ListResult 接口
        return ListResult.result((int) result.getTotal(), voList);
    }

    @LogExecutionTime("查询资产详情")
    @Override
    public AssetVO.Response getById(Long assetId) {
        Asset asset = baseMapper.selectById(assetId);
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
    @LogExecutionTime("更新资产")
    @Override
    @Transactional
    public AssetVO.Response update(Long assetId, AssetVO.Request request) {
        // 校验：至少需要提供一个非 null 字段，否则 SQL 会变成只有 WHERE 没有 SET
        if (request.getName() == null && request.getRequestMethod() == null
                && request.getRequestProtocol() == null && request.getRequestHost() == null
                && request.getRequestPort() == null && request.getRequestPath() == null
                && request.getRequestHeader() == null && request.getDescription() == null
                && request.getStatus() == null && request.getRiskLevel() == null) {
            throw new ApiException(CommonErrorCode.PARAM_INVALID.getCode(), "至少需要提供一个更新字段",
                    HttpStatus.valueOf(CommonErrorCode.PARAM_INVALID.getHttpCode()));
        }
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
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "资产不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        Asset updated = baseMapper.selectById(assetId);
        return assetMapstruct.toResponseVO(updated);
    }

    /**
     * 删除资产。
     * <p>
     * 删除前需要检查该资产是否仍关联了分类，如果关联关系未清理则拒绝删除，
     * 避免在 {@code asset_category_mapping} 表中留下悬空引用。
     * <p>
     * ★ 添加 @Transactional：防止 selectCount 检查和 deleteById 之间的竞态条件。
     * 虽然默认 READ_COMMITTED 无法阻止并发插入 mapping，但数据库层有唯一索引 uk_asset_category 兜底，
     * 事务至少保证两个操作在同一连接内执行。
     */
    @LogExecutionTime("删除资产")
    @Override
    @Transactional
    public void delete(Long assetId) {
        Asset asset = baseMapper.selectById(assetId);
        if (asset == null) {
            throw new ApiException(CommonErrorCode.RESOURCE_NOT_FOUND.getCode(), "资产不存在",
                    HttpStatus.valueOf(CommonErrorCode.RESOURCE_NOT_FOUND.getHttpCode()));
        }

        // 检查是否还存在分类关联
        long mappingCount = mappingMapper.selectCount(
                new LambdaQueryWrapper<AssetCategoryMapping>()
                        .eq(AssetCategoryMapping::getAssetId, assetId));
        if (mappingCount > 0) {
            throw new ApiException(CommonErrorCode.OPERATION_DENIED.getCode(),
                    "该资产存在分类关联，请先移除所有关联再删除",
                    HttpStatus.valueOf(CommonErrorCode.OPERATION_DENIED.getHttpCode()));
        }

        baseMapper.deleteById(assetId);
    }
}
