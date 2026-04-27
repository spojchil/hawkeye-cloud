package com.hawkeye.asset.business.mapstruct;

import com.hawkeye.asset.common.pojo.entity.AssetCategory;
import com.hawkeye.asset.common.pojo.vo.category.CategoryVO;
import org.mapstruct.Mapper;

/**
 * 资产分类对象映射转换器（MapStruct）
 * <p>
 * 编译期自动生成 {@code AssetCategoryMapstructImpl} 实现类，
 * 通过 {@code componentModel = "spring"} 注入 Spring 容器。
 */
@Mapper(componentModel = "spring")
public interface AssetCategoryMapstruct {

    AssetCategory toEntity(CategoryVO.Request request);

    CategoryVO.Response toResponseVO(AssetCategory entity);
}
