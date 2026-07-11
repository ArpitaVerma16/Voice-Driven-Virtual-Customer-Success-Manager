package com.vcsm.service;

import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.vcsm.config.VoiceRateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.concurrent.ExecutionException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RateLimitingServiceTest {

    private RateLimitingService service;

    @Mock
    private LoadingCache<String, RateLimiter> anonymousLimiters;

    @Mock
    private LoadingCache<String, RateLimiter> authenticatedLimiters;

    @Mock
    private VoiceRateLimitConfig config;

    @BeforeEach
    public void setUp() {
        service = new RateLimitingService();
        service.anonymousLimiters = anonymousLimiters;
        service.authenticatedLimiters = authenticatedLimiters;
        service.config = config;
    }

    @Test
    public void testWithinLimitRequestsSucceed() throws ExecutionException {
        RateLimiter limiter = RateLimiter.create(10.0);
        when(anonymousLimiters.get("192.168.1.1")).thenReturn(limiter);

        assertTrue(service.isAllowed("192.168.1.1"));
    }

    @Test
    public void testAuthenticatedUserHasHigherLimit() throws ExecutionException {
        RateLimiter limiter = RateLimiter.create(50.0);
        when(authenticatedLimiters.get("user123")).thenReturn(limiter);

        Authentication auth = mock(Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getName()).thenReturn("user123");
        when(auth.getPrincipal()).thenReturn("user123");

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);

        assertTrue(service.isAllowed("192.168.1.1"));
    }

    @Test
    public void testRetryAfterHeaderPresent() {
        when(config.getWindowSizeSeconds()).thenReturn(60);
        long retryAfter = service.getRetryAfterSeconds();
        assertTrue(retryAfter > 0);
    }

    @Test
    public void testRateLimiterCaching() throws ExecutionException {
        RateLimiter limiter = RateLimiter.create(10.0);
        when(anonymousLimiters.get("192.168.1.2")).thenReturn(limiter);

        service.isAllowed("192.168.1.2");
        verify(anonymousLimiters).get("192.168.1.2");
    }
}
