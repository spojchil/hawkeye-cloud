package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_template_category")
public class VulTemplateCategory extends BaseVulEntity {

    private Long templateId;

    private Long categoryId;
}
