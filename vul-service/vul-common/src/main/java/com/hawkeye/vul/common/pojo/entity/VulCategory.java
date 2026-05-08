package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 漏洞分类（树形，parent_id 自引用）
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_category")
public class VulCategory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long categoryId;

    private String name;

    private Long parentId;

    private Integer sortOrder;

    private String description;
}
