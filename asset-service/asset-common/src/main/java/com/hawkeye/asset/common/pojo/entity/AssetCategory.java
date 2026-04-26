package com.hawkeye.asset.common.pojo.entity;

import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资产分类实体 - 继承 BaseEntity 获得通用审计字段
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AssetCategory extends BaseEntity {

    // TODO 分布式不建议使用自增的主键id，之后改吧
    /**
     * 主键ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID，null 表示顶级分类
     */
    private Long parentId;

    /**
     * 分类描述
     */
    private String description;

    // 以下字段全部继承自 BaseEntity，无需在此声明：
    // tenantId, deleted, createTime, updateTime, createBy, updateBy
}