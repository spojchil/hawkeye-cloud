package com.common.utils.mybatis;

import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;

@Intercepts(@Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}))
public class MultiTenantInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        String tenantId = RequestContext.getHeader(HeaderConstants.HEADER_TENANT_ID);
        if (tenantId != null) {
            StatementHandler handler = (StatementHandler) invocation.getTarget();
            MetaObject meta = SystemMetaObject.forObject(handler);
            MappedStatement ms = (MappedStatement) meta.getValue("delegate.mappedStatement");
            // 拦截需要多租户的 SQL
            BoundSql boundSql = handler.getBoundSql();
            String sql = boundSql.getSql();
            if (sql.toLowerCase().contains("where")) {
                sql = sql.replace("where", "where tenant_id = '" + tenantId + "' and ");
            } else {
                sql = sql + " where tenant_id = '" + tenantId + "'";
            }
            // 通过反射修改 BoundSql（或使用动态 SQL 加参数更优雅）
        }
        return invocation.proceed();
    }
}
