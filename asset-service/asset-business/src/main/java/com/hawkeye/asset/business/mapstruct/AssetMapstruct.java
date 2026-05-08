package com.hawkeye.asset.business.mapstruct;

import com.hawkeye.asset.common.pojo.entity.Asset;
import com.hawkeye.asset.common.pojo.vo.asset.AssetVO;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;
import org.mapstruct.Mapper;

/**
 * 资产对象映射转换器
 */
@Mapper(componentModel = "spring")
public interface AssetMapstruct {

    PageAssetVO.Response toListAssetVO(Asset asset);

    Asset toEntity(AssetVO.Request request);

    AssetVO.Response toResponseVO(Asset asset);
}
