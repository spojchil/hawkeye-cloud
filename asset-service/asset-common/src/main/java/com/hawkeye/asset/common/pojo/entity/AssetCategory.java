package com.hawkeye.asset.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资产分类实体（树形，parent_id 自引用）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("asset_category")
public class AssetCategory extends BaseAssetEntity {

    @TableId(type = IdType.AUTO)
    private Long categoryId;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private String description;
}