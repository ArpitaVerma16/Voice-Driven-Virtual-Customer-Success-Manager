package com.vcsm.service;

import com.vcsm.config.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.github.bucket4j.Bucket;
import java.time.Duration;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock
    private RateLimitConfig rateLimitConfig;

    @InjectMocks
    private RateLimiterService rateLimiterService;

    private RateLimitConfig realConfig;

    @BeforeEach
    void setUp() {
        realConfig = new RateLimitConfig();
        when(rateLimitConfig.createDefaultBucket()).thenAnswer(invocation ->
            realConfig.createDefaultBucket()
        );
    }

    @Test
    void tryConsume_shouldReturnTrue_whenTokensAvailable() {
        boolean allowed = rateLimiterService.tryConsume("user1");
        assertTrue(allowed);
    }

    @Test
    void tryConsume_shouldReturnFalse_whenTokensExhausted() {
        String userId = "test-user";

        for (int i = 0; i < 10; i++) {
            rateLimiterService.tryConsume(userId);
        }

        boolean allowed = rateLimiterService.tryConsume(userId);
        assertFalse(allowed);
    }

    @Test
    void tryConsume_shouldTrackUsersIndependently() {
        for (int i = 0; i < 10; i++) {
            rateLimiterService.tryConsume("user1");
        }
        assertFalse(rateLimiterService.tryConsume("user1"));

        assertTrue(rateLimiterService.tryConsume("user2"));
    }

    @Test
    void getRemainingTokens_shouldDecreaseAfterConsumption() {
        String userId = "remaining-user";
        long initial = rateLimiterService.getRemainingTokens(userId);
        assertEquals(10, initial);

        rateLimiterService.tryConsume(userId);
        long afterOne = rateLimiterService.getRemainingTokens(userId);
        assertEquals(9, afterOne);

        rateLimiterService.tryConsume(userId);
        long afterTwo = rateLimiterService.getRemainingTokens(userId);
        assertEquals(8, afterTwo);
    }

    @Test
    void resetLimit_shouldRestoreTokens() {
        String userId = "reset-user";

        for (int i = 0; i < 10; i++) {
            rateLimiterService.tryConsume(userId);
        }
        assertFalse(rateLimiterService.tryConsume(userId));

        rateLimiterService.resetLimit(userId);

        assertTrue(rateLimiterService.tryConsume(userId));
        assertEquals(9, rateLimiterService.getRemainingTokens(userId));
    }

    @Test
    void getStatus_shouldReturnCorrectLimit() {
        RateLimiterService.RateLimitStatus status = rateLimiterService.getStatus("status-user");

        assertEquals(10, status.getLimit());
        assertEquals(10, status.getRemaining());
        assertTrue(status.isCanConsume());
    }

    @Test
    void getStatus_shouldReflectConsumedTokens() {
        String userId = "status-consumed";
        rateLimiterService.tryConsume(userId);
        rateLimiterService.tryConsume(userId);
        rateLimiterService.tryConsume(userId);

        RateLimiterService.RateLimitStatus status = rateLimiterService.getStatus(userId);
        assertEquals(7, status.getRemaining());
        assertEquals(10, status.getLimit());
    }

    @Test
    void multipleUsers_shouldNotInterfere() {
        String userA = "user-a";
        String userB = "user-b";

        for (int i = 0; i < 10; i++) {
            rateLimiterService.tryConsume(userA);
        }

        assertEquals(10, rateLimiterService.getRemainingTokens(userB));
        assertTrue(rateLimiterService.tryConsume(userB));

        assertFalse(rateLimiterService.tryConsume(userA));
        assertEquals(0, rateLimiterService.getRemainingTokens(userA));
    }

    @Test
    void resetLimit_shouldOnlyResetSpecifiedUser() {
        String userA = "reset-a";
        String userB = "reset-b";

        for (int i = 0; i < 10; i++) {
            rateLimiterService.tryConsume(userA);
            rateLimiterService.tryConsume(userB);
        }

        rateLimiterService.resetLimit(userA);

        assertTrue(rateLimiterService.tryConsume(userA));
        assertFalse(rateLimiterService.tryConsume(userB));
    }

    @Test
    void consecutiveResets_shouldWorkCorrectly() {
        String userId = "multi-reset";

        rateLimiterService.tryConsume(userId);
        rateLimiterService.resetLimit(userId);

        assertEquals(10, rateLimiterService.getRemainingTokens(userId));

        rateLimiterService.tryConsume(userId);
        rateLimiterService.tryConsume(userId);
        rateLimiterService.resetLimit(userId);

        assertEquals(10, rateLimiterService.getRemainingTokens(userId));
    }
}
