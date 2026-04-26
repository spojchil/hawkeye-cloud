package com.common.utils.pojo.entity;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类
 */
@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建时间，数据库管理
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人，由MyBatis拦截器填充
     */
    private Long createBy;

    /**
     * 更新人
     */
    private Long updateBy;

    /**
     * 租户id，
     */
    private Long tenantId = 1L;  // 默认1，拦截器可覆盖

    /**
     * 删除标识
     */
    private Boolean deleted = false;  // 字段名前最好不要有is会有混乱
}
