package com.common.utils.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

/**
 * 多租户拦截器——通过 JSqlParser 修改 AST，自动注入 tenant_id 条件
 * MyBatis-Plus 3.5.14+ 从 mybatis-plus-extension 移到 mybatis-plus-jsqlparser，pom.xml 需额外引入。
 * Admin（租户 0）跳过所有过滤。普通租户仅看自身数据。
 * TODO 普通租户也应可见平台数据（tenant_id=0），需 SQL 改写为 OR 条件，暂未实现。
 */
@Component
public class MultiTenantInterceptor implements TenantLineHandler {

    @Override
    public Expression getTenantId() {
        String tenantId = RequestContext.getHeader(HeaderConstants.HEADER_TENANT_ID);
        /* 无租户上下文时默认 0（平台），避免定时任务等场景 NPE */
        return new LongValue(tenantId != null ? Long.parseLong(tenantId) : 0L);
    }

    @Override
    public String getTenantIdColumn() {
        return TenantLineHandler.super.getTenantIdColumn();
    }

    @Override
    public boolean ignoreTable(String tableName) {
        String tenantId = RequestContext.getHeader(HeaderConstants.HEADER_TENANT_ID);
        /* Admin（租户 0）跳过所有过滤，可查看全量数据 */
        if ("0".equals(tenantId)) {
            return true;
        }
        /* account 表跳过：用户名全局唯一，登录时无租户上下文 */
        return "account".equals(tableName);
    }
}
