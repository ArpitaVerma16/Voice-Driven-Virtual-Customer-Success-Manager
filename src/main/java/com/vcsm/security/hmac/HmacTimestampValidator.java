package com.vcsm.security.hmac;

import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class HmacTimestampValidator {

    public static final long ALLOWED_TIMESTAMP_TOLERANCE_SECONDS = 300;

    public boolean isValid(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return false;
        }

        try {
            long requestTime = Long.parseLong(timestamp);
            long currentTime = Instant.now().getEpochSecond();

            return Math.abs(currentTime - requestTime)
                    <= ALLOWED_TIMESTAMP_TOLERANCE_SECONDS;

        } catch (NumberFormatException exception) {
            return false;
        }
    }
}