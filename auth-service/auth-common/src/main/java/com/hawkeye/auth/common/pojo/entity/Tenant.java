package com.hawkeye.auth.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("tenant")
public class Tenant extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long tenantId;

    private String name;

    private String contactEmail;

    private Integer status;

    private Integer maxAssets;

    private Integer maxUsers;

    private Integer maxTasks;
}
