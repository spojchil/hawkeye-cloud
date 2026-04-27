package com.hawkeye.asset.business.mapstruct;

import com.hawkeye.asset.common.pojo.DTO.AssetPageQueryDTO;
import com.hawkeye.asset.common.pojo.entity.Asset;
import com.hawkeye.asset.common.pojo.vo.asset.AssetVO;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;
import org.mapstruct.Mapper;

/**
 * 资产对象映射转换器（MapStruct）
 * <p>
 * 编译期自动生成 {@code AssetMapstructImpl} 实现类，
 * 通过 {@code componentModel = "spring"} 注入 Spring 容器。
 * <p>
 * 命名约定：
 * <ul>
 *   <li>{@code toEntity()} —— VO → Entity</li>
 *   <li>{@code toResponseVO()} —— Entity → VO（完整详情）</li>
 *   <li>{@code toListAssetVO()} —— Entity → VO（分页列表精简）</li>
 *   <li>{@code toAssetPageQueryDTO()} —— VO → DTO（分页参数转换）</li>
 * </ul>
 */
@Mapper(componentModel = "spring")
public interface AssetMapstruct {

    AssetPageQueryDTO toAssetPageQueryDTO(PageAssetVO.Request request);

    PageAssetVO.Response toListAssetVO(Asset asset);

    Asset toEntity(AssetVO.Request request);

    AssetVO.Response toResponseVO(Asset asset);
}
