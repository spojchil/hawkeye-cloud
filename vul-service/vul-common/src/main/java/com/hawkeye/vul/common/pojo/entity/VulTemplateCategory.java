package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板-分类关联
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_template_category")
public class VulTemplateCategory extends BaseEntity {

    private Long templateId;
    private Long categoryId;
}
