package com.vcsm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class ResponseCacheService {

    private static final long CACHE_TTL = 3600; // 1 hour in seconds
    private static final double MIN_CONFIDENCE_FOR_CACHE = 0.95;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, String> fallbackCache = new HashMap<>();

    public String getCachedResponse(String intent, Map<String, Object> entities, double confidence) {
        if (confidence < MIN_CONFIDENCE_FOR_CACHE) {
            return null;
        }

        String cacheKey = generateCacheKey(intent, entities);

        if (redisTemplate != null) {
            return redisTemplate.opsForValue().get(cacheKey);
        }

        return fallbackCache.get(cacheKey);
    }

    public void cacheResponse(String intent, Map<String, Object> entities, String response, double confidence) {
        if (confidence < MIN_CONFIDENCE_FOR_CACHE) {
            return;
        }

        String cacheKey = generateCacheKey(intent, entities);

        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL, TimeUnit.SECONDS);
        } else {
            fallbackCache.put(cacheKey, response);
        }
    }

    @CacheEvict(value = "intentResponses", allEntries = true)
    public void invalidateAllCache() {
        if (redisTemplate != null) {
            redisTemplate.getConnectionFactory().getConnection().flushAll();
        } else {
            fallbackCache.clear();
        }
    }

    public void invalidateCache(String intent) {
        if (redisTemplate != null) {
            redisTemplate.delete(redisTemplate.keys("intent:" + intent + ":*"));
        } else {
            fallbackCache.entrySet().removeIf(entry -> entry.getKey().startsWith("intent:" + intent));
        }
    }

    private String generateCacheKey(String intent, Map<String, Object> entities) {
        try {
            String keyString = "intent:" + intent + ":" +
                             objectMapper.writeValueAsString(entities);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyString.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "intent:" + intent + ":" + entities.hashCode();
        }
    }

    public boolean isCachingEnabled() {
        return redisTemplate != null && redisTemplate.getConnectionFactory() != null;
    }
}
