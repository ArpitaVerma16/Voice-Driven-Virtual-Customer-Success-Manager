package com.vcsm.security;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FraudAlertService {

    private final List<FraudAlert> alerts = new ArrayList<>();
    private final Map<String, Integer> userSuspicionScore = new ConcurrentHashMap<>();

    /**
     * Raise a fraud alert
     */
    public FraudAlert raiseAlert(String userId, String reason, String severity) {
        FraudAlert alert = new FraudAlert(
            UUID.randomUUID().toString(),
            userId,
            reason,
            severity,
            LocalDateTime.now()
        );
        alerts.add(alert);

        // Increase suspicion score
        int score = userSuspicionScore.getOrDefault(userId, 0);
        score += getSeverityScore(severity);
        userSuspicionScore.put(userId, score);

        // Auto-block if score too high
        if (score > 50) {
            alert.setAction("BLOCKED");
            alert.setMessage("User automatically blocked due to high suspicion score");
        }

        return alert;
    }

    private int getSeverityScore(String severity) {
        switch (severity) {
            case "CRITICAL": return 30;
            case "HIGH": return 20;
            case "MEDIUM": return 10;
            case "LOW": return 5;
            default: return 0;
        }
    }

    /**
     * Get alerts for user
     */
    public List<FraudAlert> getUserAlerts(String userId) {
        return alerts.stream()
            .filter(a -> a.getUserId().equals(userId))
            .toList();
    }

    /**
     * Get suspicion score for user
     */
    public int getSuspicionScore(String userId) {
        return userSuspicionScore.getOrDefault(userId, 0);
    }

    /**
     * Reset suspicion score
     */
    public void resetSuspicionScore(String userId) {
        userSuspicionScore.put(userId, 0);
    }

    /**
     * Get all alerts
     */
    public List<FraudAlert> getAllAlerts() {
        return new ArrayList<>(alerts);
    }

    public static class FraudAlert {
        private final String alertId;
        private final String userId;
        private final String reason;
        private final String severity;
        private final LocalDateTime timestamp;
        private String action = "PENDING";
        private String message = "Alert raised for review";

        public FraudAlert(String alertId, String userId, String reason, String severity, LocalDateTime timestamp) {
            this.alertId = alertId;
            this.userId = userId;
            this.reason = reason;
            this.severity = severity;
            this.timestamp = timestamp;
        }

        public String getAlertId() { return alertId; }
        public String getUserId() { return userId; }
        public String getReason() { return reason; }
        public String getSeverity() { return severity; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}