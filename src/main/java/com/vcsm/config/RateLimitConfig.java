package com.vcsm.config;

import com.bucket4j.Bucket4j;
import com.bucket4j.local.LocalBucket;
import com.bucket4j.local.LocalBucketBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @SuppressWarnings("unchecked")
    @Bean
    public Cache<String, LocalBucket> bucketCache() {
        final java.util.Map<String, LocalBucket> map = new java.util.concurrent.ConcurrentHashMap<>();
        return (Cache<String, LocalBucket>) java.lang.reflect.Proxy.newProxyInstance(
            Cache.class.getClassLoader(),
            new Class<?>[]{Cache.class},
            (proxy, method, args) -> {
                if ("get".equals(method.getName())) {
                    return map.get(args[0]);
                } else if ("put".equals(method.getName())) {
                    map.put((String) args[0], (LocalBucket) args[1]);
                    return null;
                } else if ("remove".equals(method.getName())) {
                    return map.remove(args[0]) != null;
                } else if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                } else if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                } else if ("toString".equals(method.getName())) {
                    return "CacheProxy";
                }
                return null;
            }
        );
    }

    public LocalBucket createBucket(int capacity, Duration refillDuration, int refillTokens) {
        LocalBucketBuilder builder = Bucket4j.builder()
            .addLimit(limit -> limit
                .capacity(capacity)
                .refillIntervally(refillTokens, refillDuration)
            );
        return builder.build();
    }

    public LocalBucket createDefaultBucket() {
        // 10 requests per minute
        return createBucket(10, Duration.ofMinutes(1), 10);
    }
}