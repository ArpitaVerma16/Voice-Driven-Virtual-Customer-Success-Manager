package com.vcsm.filter;

import com.vcsm.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@lombok.RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Apply rate limiting to voice and auth endpoints
        if (!path.startsWith("/api/voice/") && !path.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get key (from auth, request param, header, or client IP)
        String key = getUserId(request);
        if (key == null) {
            key = getClientIp(request);
        }

        // Check rate limit
        boolean allowed = rateLimiterService.tryConsume(key);

        if (!allowed) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("""
                {
                    "success": false,
                    "error": "Too many requests",
                    "message": "Rate limit exceeded. Please wait before trying again.",
                    "limit": 10,
                    "retryAfter": 60
                }
            """);
            return;
        }

        // Add rate limit headers
        RateLimiterService.RateLimitStatus status = rateLimiterService.getStatus(key);
        response.setHeader("X-RateLimit-Limit", String.valueOf(status.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemaining()));

        filterChain.doFilter(request, response);
    }

    private String getUserId(HttpServletRequest request) {
        // Try to get from authentication
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }

        // Try from request parameter
        String userId = request.getParameter("userId");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        // Try from header
        userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isEmpty()) {
            return userId;
        }

        return null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}