package com.common.utils.pojo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 实体基类
 * JPA仅为注释
 * 实际表结构由SQL文件定义
 */
@Data
@MappedSuperclass  // 表示这是一个映射超类，不会对应单独的表
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 创建时间，数据库管理
     */
    @Column(name = "create_time", nullable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(name = "update_time", nullable = false,
            columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updateTime;

    /**
     * 创建人，由MyBatis拦截器填充
     */
    @Column(name = "create_by")
    private Long createBy;

    /**
     * 更新人
     */
    @Column(name = "update_by")
    private Long updateBy;

    /**
     * 租户id，
     */
    @Column(name = "tenant_id", nullable = false,
            columnDefinition = "BIGINT DEFAULT 1")
    private Long tenantId = 1L;  // 默认1，拦截器可覆盖

    /**
     * 删除标识
     */
    @Column(name = "deleted", nullable = false,
            columnDefinition = "TINYINT DEFAULT 0")
    private Boolean deleted = false;  // 字段名前最好不要有is会有混乱
}
