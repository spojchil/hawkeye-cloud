package com.hawkeye.asset.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hawkeye.asset.common.pojo.entity.AssetCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产分类表
 */
@Mapper
public interface AssetCategoryMapper extends BaseMapper<AssetCategory> {
}
