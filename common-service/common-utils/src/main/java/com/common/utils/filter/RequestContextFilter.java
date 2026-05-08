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
 * 请求上下文过滤器——提取请求头存入 ThreadLocal，finally 清理防内存泄漏
 */
@Component("tenantRequestContextFilter")
@Order(1)
public class RequestContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        HttpServletRequest httpReq = (HttpServletRequest) request;

        Map<String, String> headerMap = Collections.list(httpReq.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(Function.identity(), httpReq::getHeader));

        try {
            RequestContext.setHeaders(headerMap);
            chain.doFilter(request, response);
        } finally {
            /* 无论成败都必须清理——Tomcat 线程池复用会污染下个请求 */
            RequestContext.clear();
        }
    }
}
