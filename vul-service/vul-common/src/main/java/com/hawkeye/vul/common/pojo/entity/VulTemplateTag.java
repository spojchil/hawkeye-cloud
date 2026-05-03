package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_template_tag")
public class VulTemplateTag extends BaseVulEntity {

    private Long templateId;

    private Long tagId;
}
