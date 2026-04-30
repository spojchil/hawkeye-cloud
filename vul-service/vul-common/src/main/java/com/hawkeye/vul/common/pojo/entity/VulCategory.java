package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 漏洞分类（树形结构）。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_category")
public class VulCategory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long categoryId;

    private String name;

    /** NULL = 根节点 */
    private Long parentId;

    private Integer sortOrder;

    private String description;
}
