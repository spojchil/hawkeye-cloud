package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板-分类关联（M2M）。
 * 唯一约束在 (template_id, category_id)。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_template_category")
public class VulTemplateCategory extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private Long categoryId;
}
