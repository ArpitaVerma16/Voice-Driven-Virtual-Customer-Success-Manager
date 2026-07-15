package com.vcsm.filter;

import com.vcsm.service.RateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testDoFilterInternal_NonRateLimitedPath() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/other/path");

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        verify(rateLimiterService, never()).tryConsume(anyString());
    }

    @Test
    void testDoFilterInternal_AllowedRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/voice/command");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        when(rateLimiterService.tryConsume("192.168.1.1")).thenReturn(true);
        
        RateLimiterService.RateLimitStatus status = new RateLimiterService.RateLimitStatus(9L, true, 10);
        when(rateLimiterService.getStatus("192.168.1.1")).thenReturn(status);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setHeader("X-RateLimit-Limit", "10");
        verify(response, times(1)).setHeader("X-RateLimit-Remaining", "9");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilterInternal_BlockedRequest() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/auth/login");
        when(request.getRemoteAddr()).thenReturn("192.168.1.1");
        
        when(rateLimiterService.tryConsume("192.168.1.1")).thenReturn(false);

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        rateLimitFilter.doFilterInternal(request, response, filterChain);

        verify(response, times(1)).setStatus(429);
        verify(response, times(1)).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
    }
}
