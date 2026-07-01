package com.vcsm.service;

import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.vcsm.config.VoiceRateLimitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.concurrent.ExecutionException;

@Service
public class RateLimitingService {

    @Autowired
    @Qualifier("voiceApiRateLimiter")
    private LoadingCache<String, RateLimiter> anonymousLimiters;

    @Autowired
    @Qualifier("authenticatedVoiceApiRateLimiter")
    private LoadingCache<String, RateLimiter> authenticatedLimiters;

    @Autowired
    private VoiceRateLimitConfig config;

    public boolean isAllowed(String clientIp) throws ExecutionException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            String userId = auth.getName();
            RateLimiter limiter = authenticatedLimiters.get(userId);
            return limiter.tryAcquire();
        }

        RateLimiter limiter = anonymousLimiters.get(clientIp);
        return limiter.tryAcquire();
    }

    public long getRetryAfterSeconds() {
        return Math.max(1, config.getWindowSizeSeconds() / 10);
    }

    public double getAppliedRateLimit(String clientIp) throws ExecutionException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            return config.getAuthenticatedRatePerSecond();
        }

        return config.getAnonymousRatePerSecond();
    }
}
