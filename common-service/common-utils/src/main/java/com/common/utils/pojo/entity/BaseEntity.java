package com.common.utils.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    // ★ tenantId 由 MultiTenantInterceptor（TenantLineHandler）自动注入，
    //    此处默认值仅为 fallback，实际值由请求头 X-TENANT-ID 决定
    private Long tenantId = 1L;

    // ★ @TableLogic + application.yml 中 logic-delete-field: deleted 二选一即可，
    //    同时配置以注解为准。deleted = 0 为未删，1 为已删。
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Boolean deleted = false;
}
