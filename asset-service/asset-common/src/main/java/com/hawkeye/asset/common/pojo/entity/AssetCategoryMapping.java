package com.hawkeye.asset.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资产-分类关联表（M2M）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("asset_category_mapping")
public class AssetCategoryMapping extends BaseEntity {

    private Long assetId;
    private Long categoryId;
}