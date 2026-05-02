package com.hawkeye.asset.common.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AssetMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "createBy", this::getCurrentAccountId, Long.class);
        this.strictInsertFill(metaObject, "updateBy", this::getCurrentAccountId, Long.class);
        this.strictInsertFill(metaObject, "deletedAt", () -> 0L, Long.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictUpdateFill(metaObject, "updateBy", this::getCurrentAccountId, Long.class);
    }

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
