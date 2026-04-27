package com.common.utils.mybatis;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 全局拦截器配置
 *
 * ★ 注意拦截器注册顺序：多租户 → 分页
 *    TenantLineInnerInterceptor 会先改写 SQL 加入 tenant_id 条件，
 *    然后 PaginationInnerInterceptor 再对改写后的 SQL 做分页包装，
 *    顺序不能反，否则分页 SQL 中不会包含租户条件。
 */
@Configuration
@RequiredArgsConstructor
public class MybatisPlusConfig {

    private final MultiTenantInterceptor multiTenantInterceptor;

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(multiTenantInterceptor));
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }
}
