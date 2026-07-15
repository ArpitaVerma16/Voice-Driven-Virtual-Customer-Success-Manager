package com.vcsm.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service
public class PiiRedactionService {

    // Common Regex patterns for PII
    private static final Pattern SSN_PATTERN = Pattern.compile("\\b\\d{3}[- .]?\\d{2}[- .]?\\d{4}\\b");
    private static final Pattern CREDIT_CARD_PATTERN = Pattern.compile("\\b(?:\\d[ -]*?){13,16}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?1[-. ]?)?\\(?([0-9]{3})\\)?[-. ]?([0-9]{3})[-. ]?([0-9]{4})\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+");

    public String redactPii(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String redacted = text;

        // Mask SSNs
        redacted = SSN_PATTERN.matcher(redacted).replaceAll("[SSN REDACTED]");

        // Mask Credit Cards
        redacted = CREDIT_CARD_PATTERN.matcher(redacted).replaceAll("[CREDIT CARD REDACTED]");

        // Mask Emails
        redacted = EMAIL_PATTERN.matcher(redacted).replaceAll("[EMAIL REDACTED]");

        // Mask Phone Numbers
        redacted = PHONE_PATTERN.matcher(redacted).replaceAll("[PHONE REDACTED]");

        return redacted;
    }
}
