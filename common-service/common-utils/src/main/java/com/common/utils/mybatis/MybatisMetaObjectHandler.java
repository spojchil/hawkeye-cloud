package com.common.utils.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充处理器
 * <p>
 * ★ 配合 BaseEntity 中的 @TableField(fill = FieldFill.INSERT/INSERT_UPDATE) 使用。
 *    如果填充不生效，检查两点：
 *    1. BaseEntity 上的 @TableField 注解字段名是否与此处 setFieldValByName 的字段名一致
 *    2. strictInsertFill/strictUpdateFill 要求实体字段类型与 lambda 返回值类型精确匹配
 * <p>
 * <b>createBy / updateBy 获取策略：</b> 从请求头 X-ACCOUNT-ID 中读取当前操作人 ID，
 * 通过 {@link RequestContext} ThreadLocal 传递。内部调用 / 定时任务等无请求头场景默认填 null。
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "createBy", this::getCurrentAccountId, Long.class);
        this.strictInsertFill(metaObject, "updateBy", this::getCurrentAccountId, Long.class);
        this.strictInsertFill(metaObject, "deleted", () -> false, Boolean.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictUpdateFill(metaObject, "updateBy", this::getCurrentAccountId, Long.class);
    }

    /**
     * 从请求上下文中获取当前操作人 ID。
     * 如果请求头中没有 X-ACCOUNT-ID（如内部调用、定时任务），返回 null。
     */
    private Long getCurrentAccountId() {
        String accountId = RequestContext.getHeader(HeaderConstants.HEADER_ACCOUNT_ID);
        if (accountId != null) {
            try {
                return Long.parseLong(accountId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
