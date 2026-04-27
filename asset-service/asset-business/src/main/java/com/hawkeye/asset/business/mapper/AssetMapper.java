package com.hawkeye.asset.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hawkeye.asset.common.pojo.entity.Asset;
import org.apache.ibatis.annotations.Mapper;

/**
 * 资产表 Mapper
 * <p>
 * 继承 MyBatis-Plus 的 {@link BaseMapper}，自动获得 CRUD 方法。
 * 复杂查询通过 {@link com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper} 动态构建。
 */
@Mapper
public interface AssetMapper extends BaseMapper<Asset> {
}
