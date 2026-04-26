package com.hawkeye.asset.business.service.impl;

import com.common.utils.response.ListResult;
import com.github.pagehelper.PageHelper;
import com.hawkeye.asset.business.mapper.AssetMapper;
import com.hawkeye.asset.business.service.AssetService;
import com.hawkeye.asset.common.pojo.DTO.AssetPageQueryDTO;
import com.hawkeye.asset.common.pojo.entity.Asset;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetMapper assetMapper;

    @Override
    public ListResult<PageAssetVO.Response> pageQuery(AssetPageQueryDTO assetPageQueryDTO) {
        PageHelper.startPage(assetPageQueryDTO.getPage(), assetPageQueryDTO.getPageSize());
        List<Asset> assets = assetMapper.pageQuery(assetPageQueryDTO);
    }
}
