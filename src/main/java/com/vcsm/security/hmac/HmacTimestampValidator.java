package com.vcsm.security.hmac;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HmacTimestampValidatorTest {

    private final HmacTimestampValidator validator =
            new HmacTimestampValidator();

    @Test
    void shouldAcceptCurrentTimestamp() {
        String timestamp =
                String.valueOf(Instant.now().getEpochSecond());

        assertTrue(validator.isValid(timestamp));
    }

    @Test
    void shouldRejectExpiredTimestamp() {
        long timestamp = Instant.now().getEpochSecond() - 301;

        assertFalse(
                validator.isValid(String.valueOf(timestamp))
        );
    }

    @Test
    void shouldRejectFutureTimestamp() {
        long timestamp = Instant.now().getEpochSecond() + 301;

        assertFalse(
                validator.isValid(String.valueOf(timestamp))
        );
    }

    @Test
    void shouldRejectMalformedTimestamp() {
        assertFalse(validator.isValid("invalid-timestamp"));
    }

    @Test
    void shouldRejectNullTimestamp() {
        assertFalse(validator.isValid(null));
    }

    @Test
    void shouldRejectBlankTimestamp() {
        assertFalse(validator.isValid(" "));
    }
}