package com.hawkeye.asset.business.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.common.utils.response.ListResult;
import com.hawkeye.asset.business.mapper.AssetMapper;
import com.hawkeye.asset.business.service.AssetService;
import com.hawkeye.asset.common.pojo.DTO.AssetPageQueryDTO;
import com.hawkeye.asset.common.pojo.entity.Asset;
import com.hawkeye.asset.common.pojo.vo.asset.PageAssetVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl extends ServiceImpl<AssetMapper, Asset> implements AssetService {

    private final AssetMapper assetMapper;

    @Override
    public ListResult<PageAssetVO.Response> pageQuery(AssetPageQueryDTO assetPageQueryDTO) {
        Page<Asset> page = new Page<>(assetPageQueryDTO.getPage(), assetPageQueryDTO.getPageSize());
        assetMapper.pageQuery(page, assetPageQueryDTO);
        return null;
    }
}
