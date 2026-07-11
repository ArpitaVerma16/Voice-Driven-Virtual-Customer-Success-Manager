package com.vcsm.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensitiveDataSanitizerTest {

    @Test
    void shouldRedactBearerToken() {
        String input = "Authorization: Bearer secret-token-value";

        String result = SensitiveDataSanitizer.sanitize(input);

        assertFalse(result.contains("secret-token-value"));
        assertTrue(result.contains("[REDACTED]"));
    }

    @Test
    void shouldRedactJwtToken() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyIn0.signature";
        String input = "Invalid JWT: " + jwt;

        String result = SensitiveDataSanitizer.sanitize(input);

        assertFalse(result.contains(jwt));
        assertTrue(result.contains("[REDACTED]"));
    }

    @Test
    void shouldRedactApiKey() {
        String input = "api_key=super-secret-key";

        String result = SensitiveDataSanitizer.sanitize(input);

        assertFalse(result.contains("super-secret-key"));
        assertTrue(result.contains("[REDACTED]"));
    }

    @Test
    void shouldRedactHmacSignature() {
        String input = "X-Signature: abc123signature";

        String result = SensitiveDataSanitizer.sanitize(input);

        assertFalse(result.contains("abc123signature"));
        assertTrue(result.contains("[REDACTED]"));
    }

    @Test
    void shouldPreserveNormalLogMessage() {
        String input = "Authentication request rejected";

        String result = SensitiveDataSanitizer.sanitize(input);

        assertEquals(input, result);
    }

    @Test
    void shouldHandleNullMessage() {
        assertNull(SensitiveDataSanitizer.sanitize(null));
    }
}
