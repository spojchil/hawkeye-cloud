package com.hawkeye.asset.common.pojo.entity;

import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资产-分类关联实体 - 继承 BaseEntity 获得通用审计字段
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class AssetCategoryMapping extends BaseEntity {

    // TODO 分布式不建议使用自增的主键id，之后改吧
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 资产ID
     */
    private Long assetId;

    /**
     * 分类ID
     */
    private Long categoryId;

    // 以下字段全部继承自 BaseEntity，无需在此声明：
    // tenantId, deleted, createTime, updateTime, createBy, updateBy
}