package com.vcsm.healing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AutoRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(AutoRecoveryService.class);

    private final Map<String, RecoveryAction> activeRecoveries = new ConcurrentHashMap<>();
    private final List<String> recoveryHistory = new ArrayList<>();

    /**
     * Execute recovery action for anomaly
     */
    public RecoveryResult executeRecovery(AnomalyDetector.Anomaly anomaly) {
        RecoveryAction action = determineRecoveryAction(anomaly);
        String recoveryId = UUID.randomUUID().toString();

        activeRecoveries.put(recoveryId, action);

        try {
            boolean success = performRecovery(action);
            String status = success ? "SUCCESS" : "FAILED";

            String logEntry = String.format("[%s] Recovery %s: %s - %s",
                new Date(), recoveryId, status, action.getDescription());
            recoveryHistory.add(logEntry);

            return new RecoveryResult(recoveryId, status, action, logEntry);
        } catch (RuntimeException e) {
            log.error("Recovery {} failed for action {}: {}", recoveryId, action.getType(), e.getMessage(), e);
            return new RecoveryResult(recoveryId, "FAILED", action, "Recovery failed: " + e.getMessage());
        }
    }

    private RecoveryAction determineRecoveryAction(AnomalyDetector.Anomaly anomaly) {
        String metric = anomaly.getMetricName();

        if (metric.contains("error_rate") || metric.contains("failure")) {
            return new RecoveryAction("CIRCUIT_BREAKER", "Circuit breaker opened to prevent cascading failure");
        } else if (metric.contains("response_time") || metric.contains("latency")) {
            return new RecoveryAction("SCALE_UP", "Scaling up resources to handle increased load");
        } else if (metric.contains("memory") || metric.contains("cpu")) {
            return new RecoveryAction("RESTART", "Restarting service to free up resources");
        } else if (metric.contains("thread") || metric.contains("pool")) {
            return new RecoveryAction("OPTIMIZE", "Optimizing thread pool configuration");
        } else {
            return new RecoveryAction("RETRY", "Retry operation with exponential backoff");
        }
    }

    private boolean performRecovery(RecoveryAction action) {
        // Simulate recovery execution
        try {
            // In production, actual recovery logic here
            Thread.sleep(100);
            return true;
        } catch (InterruptedException e) {
            log.warn("Recovery execution interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Get recovery status
     */
    public RecoveryStatus getRecoveryStatus(String recoveryId) {
        RecoveryAction action = activeRecoveries.get(recoveryId);
        if (action == null) {
            return new RecoveryStatus("NOT_FOUND", "Recovery not found", null);
        }

        return new RecoveryStatus(action.getType(), "In progress", action);
    }

    /**
     * Get recovery history
     */
    public List<String> getRecoveryHistory() {
        return new ArrayList<>(recoveryHistory);
    }

    public static class RecoveryAction {
        private final String type;
        private final String description;

        public RecoveryAction(String type, String description) {
            this.type = type;
            this.description = description;
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
    }

    public static class RecoveryResult {
        private final String recoveryId;
        private final String status;
        private final RecoveryAction action;
        private final String message;

        public RecoveryResult(String recoveryId, String status, RecoveryAction action, String message) {
            this.recoveryId = recoveryId;
            this.status = status;
            this.action = action;
            this.message = message;
        }

        public String getRecoveryId() { return recoveryId; }
        public String getStatus() { return status; }
        public RecoveryAction getAction() { return action; }
        public String getMessage() { return message; }
    }

    public static class RecoveryStatus {
        private final String status;
        private final String message;
        private final RecoveryAction action;

        public RecoveryStatus(String status, String message, RecoveryAction action) {
            this.status = status;
            this.message = message;
            this.action = action;
        }

        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public RecoveryAction getAction() { return action; }
    }
}