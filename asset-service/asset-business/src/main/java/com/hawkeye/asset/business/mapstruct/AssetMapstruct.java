package com.hawkeye.asset.business.mapstruct;

import com.hawkeye.asset.common.pojo.DTO.AssetPageQueryDTO;
import com.hawkeye.asset.common.pojo.entity.Asset;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;
import org.mapstruct.Mapper;

/**
 * 资产实体、VO和DTO的映射接口
 */
@Mapper
public interface AssetMapstruct {

    /**
     * VO映射DTO
     */
    AssetPageQueryDTO toAssetPageQueryDTO(PageAssetVO.Request request);

    /**
     * 实体映射VO
     * 对于不同名的属性可以使用Mapping注解指定
     * 忽略属性可以使用@Mapping
     * 如@Mapping(source = "id", target = "assetId")
     * \@Mapping(target = "password", ignore = true)
     */
    PageAssetVO.Response toListAssetVO(Asset asset);
}
