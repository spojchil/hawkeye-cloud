package com.hawkeye.asset.business.mapstruct;

import com.hawkeye.asset.common.pojo.entity.AssetCategory;
import com.hawkeye.asset.common.pojo.vo.category.CategoryVO;
import org.mapstruct.Mapper;

/**
 * 资产分类对象映射转换器
 */
@Mapper(componentModel = "spring")
public interface AssetCategoryMapstruct {

    AssetCategory toEntity(CategoryVO.Request request);

    CategoryVO.Response toResponseVO(AssetCategory entity);
}
