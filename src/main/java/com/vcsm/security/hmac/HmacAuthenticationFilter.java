package com.vcsm.security.hmac;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

@Component
public class HmacAuthenticationFilter extends OncePerRequestFilter {

    private final SignatureValidator signatureValidator;
    private final NonceCacheService nonceCacheService;

    public HmacAuthenticationFilter(
            SignatureValidator signatureValidator,
            NonceCacheService nonceCacheService) {

        this.signatureValidator = signatureValidator;
        this.nonceCacheService = nonceCacheService;
    }

    private static final Set<String> PROTECTED_PATHS =
            Set.of("/api/voice/command");

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request) {

        return !PROTECTED_PATHS.contains(
                request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String timestamp =
                request.getHeader("X-Timestamp");

        String nonce =
                request.getHeader("X-Nonce");

        String signature =
                request.getHeader("X-Signature");

        if (timestamp == null ||
                nonce == null ||
                signature == null) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing authentication headers");

            return;
        }

        long currentTime =
                Instant.now().getEpochSecond();

        long requestTime =
                Long.parseLong(timestamp);

        if (Math.abs(currentTime - requestTime) > 300) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Request expired");

            return;
        }

        if (nonceCacheService.exists(nonce)) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Replay attack detected");

            return;
        }

        CachedBodyHttpServletRequestWrapper wrappedRequest =
                new CachedBodyHttpServletRequestWrapper(request);

        String computedSignature = signatureValidator.generateSignature(
                wrappedRequest.getMethod(),
                wrappedRequest.getRequestURI(),
                wrappedRequest.getBody(),
                timestamp,
                nonce
        );

        if (!computedSignature.equals(signature)) {

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid signature");

            return;
        }

        nonceCacheService.save(nonce);

        filterChain.doFilter(wrappedRequest, response);
    }
}