package com.vcsm.service;

import io.github.bucket4j.Bucket;
import com.vcsm.config.RateLimitConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    private static final int DEFAULT_LIMIT = 10;

    @Autowired
    private RateLimitConfig rateLimitConfig;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String userId) {
        Bucket bucket = getBucket(userId);
        return bucket.tryConsume(1);
    }

    public long getRemainingTokens(String userId) {
        Bucket bucket = getBucket(userId);
        return bucket.getAvailableTokens();
    }

    public void resetLimit(String userId) {
        buckets.remove(userId);
    }

    private Bucket getBucket(String userId) {
        return buckets.computeIfAbsent(
                userId,
                id -> rateLimitConfig.createDefaultBucket()
        );
    }

    public RateLimitStatus getStatus(String userId) {
        long remaining = getRemainingTokens(userId);

        return new RateLimitStatus(
                remaining,
                remaining > 0,
                DEFAULT_LIMIT
        );
    }

    public static class RateLimitStatus {
        private final long remaining;
        private final boolean canConsume;
        private final int limit;

        public RateLimitStatus(long remaining, boolean canConsume, int limit) {
            this.remaining = remaining;
            this.canConsume = canConsume;
            this.limit = limit;
        }

        public long getRemaining() {
            return remaining;
        }

        public boolean isCanConsume() {
            return canConsume;
        }

        public int getLimit() {
            return limit;
        }
    }
}
