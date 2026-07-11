package com.vcsm.security.hmac;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class HmacAuthenticationFilter extends OncePerRequestFilter {

    private static final Set<String> PROTECTED_PATHS =
            Set.of("/api/voice/command");

    private final SignatureValidator signatureValidator;
    private final NonceCacheService nonceCacheService;
    private final HmacTimestampValidator timestampValidator;

    public HmacAuthenticationFilter(
            SignatureValidator signatureValidator,
            NonceCacheService nonceCacheService,
            HmacTimestampValidator timestampValidator) {

        this.signatureValidator = signatureValidator;
        this.nonceCacheService = nonceCacheService;
        this.timestampValidator = timestampValidator;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !PROTECTED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String signature = request.getHeader("X-Signature");

        if (timestamp == null || nonce == null || signature == null) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing authentication headers"
            );
            return;
        }

        // Reject expired, excessively future, or malformed timestamps
        // before continuing with HMAC request processing.
        if (!timestampValidator.isValid(timestamp)) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired request timestamp"
            );
            return;
        }

        if (nonceCacheService.exists(nonce)) {
            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Replay attack detected"
            );
            return;
        }

        nonceCacheService.save(nonce);

        filterChain.doFilter(request, response);
    }
}