package com.common.utils.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

/**
 * 多租户拦截器 — 适配 MyBatis-Plus 3.5.14+
 *
 * ★ 坑点记录：
 * 1. 旧版（3.5.5 及之前）是 implements Interceptor，手动拦截 StatementHandler.prepare
 *    通过字符串替换 sql 添加 "where tenant_id = ?"，存在 SQL 注入风险且不支持 INSERT 自动拼装。
 * 2. MyBatis-Plus 3.5.14+ 将 TenantLineHandler/TenantLineInnerInterceptor 从
 *    mybatis-plus-extension 移到了 mybatis-plus-jsqlparser 模块，pom.xml 必须额外引入。
 * 3. 现在实现 TenantLineHandler 接口，由 TenantLineInnerInterceptor 自动通过 JSqlParser
 *    修改 AST，安全地给 SELECT/UPDATE/DELETE 加 tenant_id 条件，给 INSERT 自动填充列。
 */
@Component
public class MultiTenantInterceptor implements TenantLineHandler {

    @Override
    public Expression getTenantId() {
        String tenantId = RequestContext.getHeader(HeaderConstants.HEADER_TENANT_ID);
        return new LongValue(tenantId != null ? Long.parseLong(tenantId) : 1L);
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return false;
    }
}
