package com.vcsm.security.hmac;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.ByteArrayInputStream;
import java.time.Instant;

import static org.mockito.Mockito.*;

public class HmacAuthenticationFilterTest {

    private HmacAuthenticationFilter filter;

    @Mock
    private SignatureValidator signatureValidator;

    @Mock
    private NonceCacheService nonceCacheService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        filter = new HmacAuthenticationFilter(signatureValidator, nonceCacheService);
    }

    @Test
    void testFilterBypassedForUnprotectedPath() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/public");
        
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testMissingHeaders() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/voice/command");
        when(request.getHeader("X-Timestamp")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authentication headers");
        verifyNoInteractions(filterChain);
    }

    @Test
    void testValidSignatureSuccess() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = "unique-nonce";
        String body = "{\"command\":\"test\"}";
        String signature = "valid-signature";

        when(request.getRequestURI()).thenReturn("/api/voice/command");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Timestamp")).thenReturn(timestamp);
        when(request.getHeader("X-Nonce")).thenReturn(nonce);
        when(request.getHeader("X-Signature")).thenReturn(signature);
        
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
        when(request.getInputStream()).thenReturn(new jakarta.servlet.ServletInputStream() {
            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }
            @Override
            public boolean isReady() {
                return true;
            }
            @Override
            public void setReadListener(jakarta.servlet.ReadListener readListener) {}
        });

        when(nonceCacheService.exists(nonce)).thenReturn(false);
        when(signatureValidator.generateSignature("POST", "/api/voice/command", body, timestamp, nonce))
                .thenReturn(signature);

        filter.doFilter(request, response, filterChain);

        verify(nonceCacheService).save(nonce);
        verify(filterChain).doFilter(any(CachedBodyHttpServletRequestWrapper.class), eq(response));
    }

    @Test
    void testInvalidSignatureUnauthorized() throws Exception {
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String nonce = "unique-nonce";
        String body = "{\"command\":\"test\"}";
        String signature = "invalid-signature";

        when(request.getRequestURI()).thenReturn("/api/voice/command");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-Timestamp")).thenReturn(timestamp);
        when(request.getHeader("X-Nonce")).thenReturn(nonce);
        when(request.getHeader("X-Signature")).thenReturn(signature);

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
        when(request.getInputStream()).thenReturn(new jakarta.servlet.ServletInputStream() {
            @Override
            public int read() {
                return byteArrayInputStream.read();
            }
            @Override
            public boolean isFinished() {
                return byteArrayInputStream.available() == 0;
            }
            @Override
            public boolean isReady() {
                return true;
            }
            @Override
            public void setReadListener(jakarta.servlet.ReadListener readListener) {}
        });

        when(nonceCacheService.exists(nonce)).thenReturn(false);
        when(signatureValidator.generateSignature("POST", "/api/voice/command", body, timestamp, nonce))
                .thenReturn("expected-valid-signature");

        filter.doFilter(request, response, filterChain);

        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid signature");
        verifyNoInteractions(filterChain);
    }
}
