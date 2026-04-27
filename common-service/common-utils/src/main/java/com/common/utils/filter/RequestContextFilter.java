package com.common.utils.filter;

import com.common.utils.context.RequestContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 请求上下文过滤器
 * <p>
 * 在每个 HTTP 请求进入时，将请求头信息提取并存入 {@link RequestContext}（ThreadLocal），
 * 使得后续任何同线程内的代码（如 Service、Mapper、Aspect 等）都可以通过
 * {@code RequestContext.getHeader(...)} 获取请求头信息（如租户 ID、账号 ID 等），
 * 而无需逐层传递 HttpServletRequest 参数。
 * <p>
 * <b>执行时机：</b> 通过 {@code @Order(1)} 保证在过滤器链中最先执行，
 * 确保多租户拦截器等其他组件在访问 RequestContext 时数据已就绪。
 * <p>
 * <b>生命周期：</b>
 * <ul>
 *   <li>请求进入 → doFilter 开始 → RequestContext.setHeaders(headerMap) → 将数据存入 ThreadLocal</li>
 *   <li>请求处理完成 → doFilter 结束 → finally → RequestContext.clear() → 清除 ThreadLocal，防止内存泄漏</li>
 * </ul>
 * <b>为什么用 finally 清理：</b> 即使业务处理过程中抛出异常，保证 ThreadLocal 仍能被清理，
 * 避免在线程池复用场景下造成数据串用和内存泄漏。
 *
 * @see RequestContext
 */
@Component("tenantRequestContextFilter")  // 指定 Bean 名称，避免与其他模块同名 Bean 冲突
@Order(1)  // 数字越小越优先，确保 RequestContext 数据在后续拦截器/过滤器之前就绪
public class RequestContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest httpReq = (HttpServletRequest) request;

        /*
         * 将所有请求头提取为不可变 Map（headerName -> headerValue）。
         * 注意：getHeaderNames() 返回的是 Enumeration，需要转为 List 才能使用 Stream API。
         * 如果有重复请求头名，toMap 默认抛出 IllegalStateException，但 HTTP 规范允许同名头
         * 出现多次（如 Set-Cookie），后续如果遇到可改用 merge 策略。
         */
        Map<String, String> headerMap = Collections.list(httpReq.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(Function.identity(), httpReq::getHeader));

        try {
            // 将请求头存入 ThreadLocal，后续链路可通过 RequestContext 获取
            RequestContext.setHeaders(headerMap);
            // 放行到下一个过滤器/拦截器，最终到达 Controller
            chain.doFilter(request, response);
        } finally {
            // ★ 关键：无论请求处理成功还是失败，都必须清理 ThreadLocal
            // 否则在线程池（Tomcat 默认线程池）复用线程时，上一个请求的数据会污染下一个请求
            RequestContext.clear();
        }
    }
}
