package com.flow.engine.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 为每个请求分配/透传请求 ID，写入响应头 X-Request-Id 与日志 MDC，
 * 并填充 {@link RequestContext} 供统一响应使用。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String reqId = request.getHeader(HEADER);
        if (!StringUtils.hasText(reqId)) {
            reqId = UUID.randomUUID().toString();
        }
        RequestContext ctx = RequestContext.current();
        ctx.setRequestId(reqId);
        MDC.put("requestId", reqId);
        response.setHeader(HEADER, reqId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
            RequestContext.clear();
        }
    }
}
