package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 漏洞标签
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_tag")
public class VulTag extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long tagId;

    private String name;
}
