package com.hawkeye.auth.common.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@TableName("account")
public class Account extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long accountId;
    private String username;
    private String password;
}
