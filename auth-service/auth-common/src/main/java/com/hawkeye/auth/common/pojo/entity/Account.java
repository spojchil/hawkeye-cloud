package com.hawkeye.auth.common.pojo.entity;

import com.common.utils.pojo.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 账号实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class Account extends BaseEntity {
    private Long accountId;
    private String username;
    private String password;
}
