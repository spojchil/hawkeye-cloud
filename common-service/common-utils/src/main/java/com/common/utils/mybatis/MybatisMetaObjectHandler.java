package com.common.utils.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "createBy", this::getCurrentUsername, String.class);
        this.strictInsertFill(metaObject, "updateBy", this::getCurrentUsername, String.class);
        this.strictInsertFill(metaObject, "deletedAt", () -> 0L, Long.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictUpdateFill(metaObject, "updateBy", this::getCurrentUsername, String.class);
    }

    private String getCurrentUsername() {
        String username = RequestContext.getHeader(HeaderConstants.HEADER_USERNAME);
        return username != null ? username : "system";
    }
}
