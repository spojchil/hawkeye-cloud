package com.hawkeye.asset.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hawkeye.asset.common.pojo.entity.AssetCategoryMapping;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产-分类关联表 Mapper
 * <p>
 * 维护资产与分类的多对多关系。
 */
@Mapper
public interface AssetCategoryMappingMapper extends BaseMapper<AssetCategoryMapping> {
}
