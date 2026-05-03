package com.common.utils.mybatis;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.common.utils.constant.HeaderConstants;
import com.common.utils.context.RequestContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

/**
 * 多租户拦截器 —— 适配 MyBatis-Plus 3.5.14+
 * <p>
 * 实现 {@link TenantLineHandler} 接口，由 {@code TenantLineInnerInterceptor}
 * 在 SQL 执行前通过 JSqlParser 修改 AST，自动为 SQL 注入租户隔离条件。
 * <p>
 * <b>工作原理：</b>
 * <ul>
 *   <li><b>SELECT/UPDATE/DELETE：</b> 自动追加 {@code WHERE tenant_id = ?} 条件</li>
 *   <li><b>INSERT：</b> 自动填充 {@code tenant_id} 列的值</li>
 *   <li><b>租户来源：</b> 从请求头 {@code X-TENANT-ID} 中获取，通过 {@link RequestContext} ThreadLocal 传递</li>
 * </ul>
 * <p>
 * ★ <b>坑点记录：</b>
 * <ol>
 *   <li>旧版（3.5.5 及之前）是 {@code implements Interceptor}，手动拦截 {@code StatementHandler.prepare}
 *       通过字符串替换 SQL 添加 {@code "where tenant_id = ?"}，存在 SQL 注入风险且不支持 INSERT 自动拼装。</li>
 *   <li>MyBatis-Plus 3.5.14+ 将 {@code TenantLineHandler} / {@code TenantLineInnerInterceptor} 从
 *       {@code mybatis-plus-extension} 移到了 {@code mybatis-plus-jsqlparser} 模块，pom.xml 必须额外引入。</li>
 *   <li>现在实现 {@code TenantLineHandler} 接口，由 {@code TenantLineInnerInterceptor} 通过 JSqlParser
 *       修改 AST，安全地给 DML 语句添加租户条件。</li>
 * </ol>
 *
 * @see RequestContext
 * @see HeaderConstants#HEADER_TENANT_ID
 */
@Component
public class MultiTenantInterceptor implements TenantLineHandler {

    /**
     * 获取当前请求的租户 ID。
     * <p>
     * 从 {@link RequestContext}（ThreadLocal 存储）中读取请求头 {@code X-TENANT-ID}。
     * 如果请求头为空（例如定时任务或内部调用），默认返回租户 ID = 1（平台默认租户）。
     * <p>
     * JSqlParser 会根据 Expression 类型决定 SQL 中是否加引号。
     *
     * @return 当前租户 ID 的 SQL 表达式
     */
    @Override
    public Expression getTenantId() {
        String tenantId = RequestContext.getHeader(HeaderConstants.HEADER_TENANT_ID);
        // 无租户 ID 时默认使用租户 1（平台默认租户），避免内部调用/定时任务等场景报 NPE
        return new LongValue(tenantId != null ? Long.parseLong(tenantId) : 1L);
    }

    /**
     * 租户 ID 在数据库表中的列名。
     * <p>
     * 所有多租户表统一使用 {@code tenant_id} 作为租户隔离列。
     */
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    /**
     * 判断是否需要跳过某些表的租户隔离。
     * <p>
     * 当前项目所有表都进行租户隔离，没有公共表，返回 {@code false} 表示不忽略任何表。
     * 如果后续需要引入全局配置表等不需要租户隔离的表，可在此方法中按表名返回 {@code true}。
     *
     * @param tableName SQL 中涉及的表名
     * @return true 表示跳过该表的租户隔离
     */
    @Override
    public boolean ignoreTable(String tableName) {
        return false;
    }
}
