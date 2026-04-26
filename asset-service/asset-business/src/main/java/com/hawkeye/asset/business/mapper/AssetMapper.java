package com.hawkeye.asset.business.mapper;

import com.hawkeye.asset.common.pojo.DTO.AssetPageQueryDTO;
import com.hawkeye.asset.common.pojo.entity.Asset;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AssetMapper {

    List<Asset> pageQuery(AssetPageQueryDTO assetPageQueryDTO);
}
