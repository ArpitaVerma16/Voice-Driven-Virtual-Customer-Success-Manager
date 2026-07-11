package com.vcsm.config;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.TimeUnit;

@Configuration
public class VoiceRateLimitConfig {

    @Value("${rate-limit.voice.anonymous:10.0}")
    private double anonymousRatePerSecond;

    @Value("${rate-limit.voice.authenticated:50.0}")
    private double authenticatedRatePerSecond;

    @Value("${rate-limit.window-size-seconds:60}")
    private int windowSizeSeconds;

    @Bean(name = "voiceApiRateLimiter")
    public LoadingCache<String, RateLimiter> voiceApiRateLimiter() {
        return CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, RateLimiter>() {
                @Override
                public RateLimiter load(String clientId) {
                    return RateLimiter.create(anonymousRatePerSecond);
                }
            });
    }

    @Bean(name = "authenticatedVoiceApiRateLimiter")
    public LoadingCache<String, RateLimiter> authenticatedVoiceApiRateLimiter() {
        return CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterAccess(1, TimeUnit.HOURS)
            .build(new CacheLoader<String, RateLimiter>() {
                @Override
                public RateLimiter load(String userId) {
                    return RateLimiter.create(authenticatedRatePerSecond);
                }
            });
    }

    public double getAnonymousRatePerSecond() {
        return anonymousRatePerSecond;
    }

    public double getAuthenticatedRatePerSecond() {
        return authenticatedRatePerSecond;
    }

    public int getWindowSizeSeconds() {
        return windowSizeSeconds;
    }
}
