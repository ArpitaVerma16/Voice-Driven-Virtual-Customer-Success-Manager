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
public class RateLimitFilter extends OncePerRequestFilter {

    @Autowired
    private RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only apply rate limiting to voice commands
        if (!path.contains("/api/voice/command")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get user ID (from session, auth, or request param)
        String userId = getUserId(request);
        if (userId == null) {
            userId = "anonymous";
        }

        // Check rate limit
        boolean allowed = rateLimiterService.tryConsume(userId);

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
        RateLimiterService.RateLimitStatus status = rateLimiterService.getStatus(userId);
        response.setHeader("X-RateLimit-Limit", String.valueOf(status.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemaining()));

        filterChain.doFilter(request, response);
    }

    private String getUserId(HttpServletRequest request) {
        // Try to get from authentication
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
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
}