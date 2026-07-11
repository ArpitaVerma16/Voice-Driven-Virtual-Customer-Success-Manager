package com.vcsm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class ResponseCacheServiceTest {

    @Autowired
    private ResponseCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService.invalidateAllCache();
    }

    @Test
    void testCacheResponseWithHighConfidence() {
        String intent = "billing_inquiry";
        Map<String, Object> entities = new HashMap<>();
        entities.put("account_id", "ACC123");
        String response = "Your current balance is $50.00";
        double confidence = 0.98;

        cacheService.cacheResponse(intent, entities, response, confidence);
        String cached = cacheService.getCachedResponse(intent, entities, confidence);

        assertEquals(response, cached);
    }

    @Test
    void testDoNotCacheLowConfidenceResponse() {
        String intent = "billing_inquiry";
        Map<String, Object> entities = new HashMap<>();
        entities.put("account_id", "ACC123");
        String response = "Your current balance is $50.00";
        double lowConfidence = 0.85;

        cacheService.cacheResponse(intent, entities, response, lowConfidence);
        String cached = cacheService.getCachedResponse(intent, entities, lowConfidence);

        assertNull(cached);
    }

    @Test
    void testInvalidateCacheByIntent() {
        String intent = "billing_inquiry";
        Map<String, Object> entities = new HashMap<>();
        entities.put("account_id", "ACC123");
        String response = "Your current balance is $50.00";

        cacheService.cacheResponse(intent, entities, response, 0.98);
        cacheService.invalidateCache(intent);
        String cached = cacheService.getCachedResponse(intent, entities, 0.98);

        assertNull(cached);
    }

    @Test
    void testCachingDisabledWhenNotConfigured() {
        assertFalse(cacheService.isCachingEnabled());
    }
}
