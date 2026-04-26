package com.hawkeye.asset.business.service;

import com.common.utils.response.ListResult;
import com.hawkeye.asset.common.pojo.DTO.AssetPageQueryDTO;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;

/**
 * 资产服务接口
 */
public interface AssetService {

    ListResult<PageAssetVO.Response> pageQuery(AssetPageQueryDTO assetPageQueryDTO);
}
