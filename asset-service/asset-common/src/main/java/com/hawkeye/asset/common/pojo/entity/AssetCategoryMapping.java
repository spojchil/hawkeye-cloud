package com.hawkeye.asset.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("asset_category_mapping")
public class AssetCategoryMapping extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long assetId;
    private Long categoryId;
}