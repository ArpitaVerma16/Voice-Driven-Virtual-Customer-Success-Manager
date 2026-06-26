package com.vcsm.security.filter;

import com.vcsm.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final RateLimiterService rateLimiterService;

    public RateLimitingFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Apply rate limiting only to public APIs mentioned in Issue #339
        if (requestURI.startsWith("/api/voice")
                || requestURI.startsWith("/api/complaints")) {

            String clientId = getClientIdentifier(request);

            if (!rateLimiterService.tryConsume(clientId)) {

                logger.warn(
                        "Rate limit exceeded for client {} on endpoint {}",
                        clientId,
                        requestURI
                );

                response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
                response.setContentType("application/json");

                response.setHeader("Retry-After", "60");

                response.getWriter().write("""
                        {
                          "status":429,
                          "error":"Too Many Requests",
                          "message":"Rate limit exceeded. Please try again after 60 seconds."
                        }
                        """);

                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIdentifier(HttpServletRequest request) {

        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}