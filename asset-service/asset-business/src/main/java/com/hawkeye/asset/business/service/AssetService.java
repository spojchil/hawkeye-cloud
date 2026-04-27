package com.hawkeye.asset.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.response.ListResult;
import com.hawkeye.asset.common.pojo.DTO.AssetPageQueryDTO;
import com.hawkeye.asset.common.pojo.entity.Asset;
import com.hawkeye.asset.common.pojo.vo.asset.AssetVO;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;

/**
 * 资产服务接口
 * <p>
 * 继承 MyBatis-Plus 的 {@link IService}，获得一批内置的 CRUD 方法。
 */
public interface AssetService extends IService<Asset> {

    ListResult<PageAssetVO.Response> pageQuery(AssetPageQueryDTO assetPageQueryDTO);

    AssetVO.Response getById(Long assetId);

    AssetVO.Response create(AssetVO.Request request);

    AssetVO.Response update(Long assetId, AssetVO.Request request);

    void delete(Long assetId);
}
