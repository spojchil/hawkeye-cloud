package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 模板-标签关联（M2M）。
 * 唯一约束在 (template_id, tag_id)，代理 id 为 MyBatis-Plus 兼容。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_template_tag")
public class VulTemplateTag extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long templateId;

    private Long tagId;
}
