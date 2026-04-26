package com.common.utils.filter;

import com.common.utils.context.RequestContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 请求上下文过滤器
 * 在每个HTTP请求的线程中设置请求上下文信息
 */
@Component
public class RequestContextFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        Map<String, String> headerMap = Collections.list(httpReq.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(Function.identity(), httpReq::getHeader));
        try {
            RequestContext.setHeaders(headerMap);
            chain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }
}
