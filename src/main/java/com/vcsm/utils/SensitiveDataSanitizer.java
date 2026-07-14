package com.vcsm.util;

import java.util.regex.Pattern;

public final class SensitiveDataSanitizer {

    private static final String REDACTED = "[REDACTED]";

    private static final Pattern BEARER_TOKEN = Pattern.compile(
            "(?i)(Bearer\\s+)[A-Za-z0-9._~+/=-]+"
    );

    private static final Pattern JWT_TOKEN = Pattern.compile(
            "\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b"
    );

    private static final Pattern API_KEY = Pattern.compile(
            "(?i)(api[-_ ]?key\\s*[:=]\\s*)[^\\s,;]+"
    );

    private static final Pattern HMAC_SIGNATURE = Pattern.compile(
            "(?i)((?:X-Signature|signature)\\s*[:=]\\s*)[^\\s,;]+"
    );

    private SensitiveDataSanitizer() {
    }

    public static String sanitize(String message) {
        if (message == null) {
            return null;
        }

        String sanitized = BEARER_TOKEN.matcher(message)
                .replaceAll("$1" + REDACTED);

        sanitized = JWT_TOKEN.matcher(sanitized)
                .replaceAll(REDACTED);

        sanitized = API_KEY.matcher(sanitized)
                .replaceAll("$1" + REDACTED);

        sanitized = HMAC_SIGNATURE.matcher(sanitized)
                .replaceAll("$1" + REDACTED);

        return sanitized;
    }
}