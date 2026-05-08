package com.hawkeye.asset.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hawkeye.asset.common.pojo.entity.Asset;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产表
 */
@Mapper
public interface AssetMapper extends BaseMapper<Asset> {
}
