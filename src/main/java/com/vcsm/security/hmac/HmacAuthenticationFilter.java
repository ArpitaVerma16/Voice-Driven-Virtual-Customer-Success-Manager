package com.vcsm.security.hmac;

import com.vcsm.util.SensitiveDataSanitizer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;

@Component
public class HmacAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(HmacAuthenticationFilter.class);

    private static final Set<String> PROTECTED_PATHS =
            Set.of("/api/voice/command");

    private final SignatureValidator signatureValidator;
    private final NonceCacheService nonceCacheService;

    public HmacAuthenticationFilter(
            SignatureValidator signatureValidator,
            NonceCacheService nonceCacheService) {

        this.signatureValidator = signatureValidator;
        this.nonceCacheService = nonceCacheService;
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
            log.warn(SensitiveDataSanitizer.sanitize(
                    "HMAC authentication rejected: missing authentication headers"
            ));

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Missing authentication headers"
            );

            return;
        }

        final long requestTime;

        try {
            requestTime = Long.parseLong(timestamp);
        } catch (NumberFormatException exception) {
            log.warn(SensitiveDataSanitizer.sanitize(
                    "HMAC authentication rejected: invalid timestamp"
            ));

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid request timestamp"
            );

            return;
        }

        long currentTime = Instant.now().getEpochSecond();

        if (Math.abs(currentTime - requestTime) > 300) {
            log.warn(SensitiveDataSanitizer.sanitize(
                    "HMAC authentication rejected: request expired"
            ));

            response.sendError(
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Request expired"
            );

            return;
        }

        if (nonceCacheService.exists(nonce)) {
            log.warn(SensitiveDataSanitizer.sanitize(
                    "HMAC authentication rejected: replay attack detected"
            ));

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