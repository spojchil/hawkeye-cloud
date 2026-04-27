package com.common.utils.mybatis;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 自动填充处理器
 *
 * ★ 配合 BaseEntity 中的 @TableField(fill = FieldFill.INSERT/INSERT_UPDATE) 使用。
 *    如果填充不生效，检查两点：
 *    1. BaseEntity 上的 @TableField 注解字段名是否与此处 setFieldValByName 的字段名一致
 *    2. strictInsertFill/strictUpdateFill 要求实体字段类型与 lambda 返回值类型精确匹配
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
        this.strictInsertFill(metaObject, "deleted", () -> false, Boolean.class);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime::now, LocalDateTime.class);
    }
}
