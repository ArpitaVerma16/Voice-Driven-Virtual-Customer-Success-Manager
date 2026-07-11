package com.vcsm.filter;

import com.vcsm.service.RateLimitingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Component
@lombok.RequiredArgsConstructor
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final RateLimitingService rateLimitingService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException, ExecutionException {

        String requestPath = request.getRequestURI();
        if (!requestPath.startsWith("/api/voice")) {
            return true;
        }

        String clientIp = getClientIp(request);

        if (!rateLimitingService.isAllowed(clientIp)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(rateLimitingService.getRetryAfterSeconds()));
            response.setHeader("X-RateLimit-Limit", String.valueOf(rateLimitingService.getAppliedRateLimit(clientIp)));

            String errorResponse = "{\"error\": \"Too many requests. Please slow down.\", " +
                    "\"status\": 429, " +
                    "\"retryAfter\": " + rateLimitingService.getRetryAfterSeconds() + "}";
            response.getWriter().write(errorResponse);

            return false;
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
