package com.hawkeye.asset.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hawkeye.asset.common.pojo.entity.AssetCategory;
import com.hawkeye.asset.common.pojo.vo.category.CategoryVO;

import java.util.List;

/**
 * 资产分类服务接口
 */
public interface AssetCategoryService extends IService<AssetCategory> {

    List<CategoryVO.Response> listCategories(Long parentId, String name);

    CategoryVO.Response create(CategoryVO.Request request);

    CategoryVO.Response update(Long categoryId, CategoryVO.Request request);

    void delete(Long categoryId);

    int addAssets(Long categoryId, List<Long> assetIds);

    int removeAssets(Long categoryId, List<Long> assetIds);
}
