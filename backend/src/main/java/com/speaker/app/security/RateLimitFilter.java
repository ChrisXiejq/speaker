package com.speaker.app.security;

import com.speaker.app.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 简单按 IP 滑动窗口限流，降低撞库、刷接口与滥用 AI 的风险。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;
    private final Map<String, Deque<Long>> authBuckets = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> apiBuckets = new ConcurrentHashMap<>();
    private final Map<String, Deque<Long>> aiBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }
        String path = request.getRequestURI();
        String ip = clientIp(request);
        int authLimit = appProperties.getSecurity().getRateLimit().getAuthPerMinute();
        int apiLimit = appProperties.getSecurity().getRateLimit().getApiPerMinute();
        int aiLimit = appProperties.getSecurity().getRateLimit().getAiPerMinute();

        if (path.startsWith("/api/auth")) {
            if (!allow(authBuckets, "auth:" + ip, authLimit)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\":\"rate_limited\"}");
                return;
            }
        } else if (path.startsWith("/api/")) {
            boolean aiHeavy = "POST".equalsIgnoreCase(request.getMethod())
                    && path.startsWith("/api/practice");
            if (aiHeavy && !allow(aiBuckets, "ai:" + ip, aiLimit)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\":\"rate_limited\"}");
                return;
            }
            if (!allow(apiBuckets, "api:" + ip, apiLimit)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.getWriter().write("{\"error\":\"rate_limited\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static boolean allow(Map<String, Deque<Long>> buckets, String key, int limitPerMinute) {
        long now = System.currentTimeMillis();
        long windowStart = now - 60_000L;
        Deque<Long> q = buckets.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && q.peekFirst() != null && q.peekFirst() < windowStart) {
                q.pollFirst();
            }
            if (q.size() >= limitPerMinute) {
                return false;
            }
            q.addLast(now);
            return true;
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String real = request.getHeader("X-Real-IP");
        if (real != null && !real.isBlank()) {
            return real.trim();
        }
        return request.getRemoteAddr();
    }
}
