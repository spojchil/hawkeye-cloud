package com.common.utils.pojo.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 租户 ID，由 TenantLineInnerInterceptor 自动注入 */
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 创建人 username，由 MetaObjectHandler 从 X-USERNAME 请求头填充 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 更新人 username，由 MetaObjectHandler 从 X-USERNAME 请求头填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    /** 软删除时间戳(毫秒), 0=未删除 */
    @TableField(fill = FieldFill.INSERT)
    private Long deletedAt;
}
