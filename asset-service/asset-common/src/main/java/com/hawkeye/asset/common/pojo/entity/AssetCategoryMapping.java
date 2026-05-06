package com.hawkeye.asset.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("asset_category_mapping")
public class AssetCategoryMapping extends BaseAssetEntity {

    private Long assetId;
    private Long categoryId;
}