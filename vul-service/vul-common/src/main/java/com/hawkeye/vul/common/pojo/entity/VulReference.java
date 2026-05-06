package com.hawkeye.vul.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("vul_reference")
public class VulReference extends BaseVulEntity {

    @TableId(type = IdType.AUTO)
    private Long referenceId;

    private Long templateId;

    private String url;

    private String title;
}
