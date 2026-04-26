package com.hawkeye.asset.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.common.utils.response.ListResult;
import com.hawkeye.asset.common.pojo.DTO.AssetPageQueryDTO;
import com.hawkeye.asset.common.pojo.entity.Asset;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;

public interface AssetService extends IService<Asset> {

    ListResult<PageAssetVO.Response> pageQuery(AssetPageQueryDTO assetPageQueryDTO);
}
