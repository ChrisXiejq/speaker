package com.speaker.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 为每条 HTTP 请求生成 requestId 写入 MDC，并记录方法、路径、耗时与状态码，便于与异常堆栈对照排查。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    public static final String MDC_REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = shortId();
        MDC.put(MDC_REQUEST_ID, requestId);
        long start = System.nanoTime();
        String query = request.getQueryString();
        String path = request.getRequestURI();
        try {
            log.debug(
                    "request start id={} {} {} query={} remote={}",
                    requestId,
                    request.getMethod(),
                    path,
                    query,
                    request.getRemoteAddr());
            filterChain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - start) / 1_000_000L;
            log.info(
                    "request done id={} {} {} -> {} ms status={}",
                    requestId,
                    request.getMethod(),
                    path,
                    ms,
                    response.getStatus());
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private static String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
