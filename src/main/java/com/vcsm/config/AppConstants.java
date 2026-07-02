package com.vcsm.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Central constants for the VCSM application.
 * Extracted from hardcoded values scattered across the codebase.
 */
public final class AppConstants {

    private AppConstants() {}

    // Email addresses
    public static final String ADMIN_EMAIL = "admin@example.com";
    public static final String SECURITY_EMAIL = "security@example.com";
    public static final String MAINTENANCE_EMAIL = "maintenance@example.com";
    public static final String SUPPORT_EMAIL = "support@example.com";

    // URLs
    public static final String LOCAL_BASE_URL = "http://localhost:8080";
    public static final String EVENTS_URL = LOCAL_BASE_URL + "/events";
    public static final String API_BASE_URL = LOCAL_BASE_URL + "/api";
    public static final String WAITLIST_CONFIRM_URL = API_BASE_URL + "/events/waitlist/confirm?eventId=";
    public static final String TWILIO_MENU_URL = "/api/twilio/menu";

    // Timeouts and delays (milliseconds)
    public static final int CACHE_REFRESH_MS = 5000;
    public static final int SCHEDULER_POLL_MS = 10000;
    public static final int SELF_HEALING_DELAY_MS = 300000;
    public static final int TOKEN_EXPIRY_MS = 3600000;

    // Voice biometrics
    public static final double VOICE_VERIFICATION_THRESHOLD = 0.75;
    public static final int VOICE_SAMPLE_RATE = 16000;
    public static final int VOICE_RECORDING_DURATION_SEC = 5;

    // Page sizes
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    // Feature flags
    public static final String FEATURE_AI_AGENT = "ai-agent";
    public static final String FEATURE_VOICE_CLONING = "voice-cloning";
    public static final String FEATURE_BLOCKCHAIN = "blockchain";
    public static final String FEATURE_SELF_HEALING = "self-healing";
    public static final String FEATURE_QUANTUM = "quantum";
    public static final String FEATURE_SNN = "snn";
    public static final String FEATURE_BCI = "bci";

    // Category urgency mapping
    public static final Map<String, Integer> CATEGORY_URGENCY = createUrgencyMap();

    private static Map<String, Integer> createUrgencyMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("billing", 5);
        map.put("technical", 4);
        map.put("account", 3);
        map.put("general", 2);
        map.put("feedback", 1);
        return Collections.unmodifiableMap(map);
    }

    // Priority keywords
    public static final String[] PRIORITY_URGENT_KEYWORDS = {"urgent", "emergency", "critical", "asap", "immediately"};
    public static final String[] PRIORITY_HIGH_KEYWORDS = {"important", "escalate", "serious", "severe"};
}
