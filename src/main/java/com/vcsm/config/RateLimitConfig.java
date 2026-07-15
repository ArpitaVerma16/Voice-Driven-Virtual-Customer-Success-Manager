package com.vcsm.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    public Bucket createBucket(int capacity, Duration refillDuration, int refillTokens) {

        Bandwidth limit = Bandwidth.classic(
                capacity,
                Refill.intervally(refillTokens, refillDuration)
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public Bucket createDefaultBucket() {
        return createBucket(10, Duration.ofMinutes(1), 10);
    }
}
