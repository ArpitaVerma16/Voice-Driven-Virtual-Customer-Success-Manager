package com.vcsm.security;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AttackDetector {

    private final Map<String, List<AttackLog>> attackLogs = new ConcurrentHashMap<>();
    private final Map<String, Integer> attackCounts = new ConcurrentHashMap<>();

    private static final int THRESHOLD = 5; // Attacks per minute

    /**
     * Detect and log attack attempts
     */
    public AttackDetectionResult detectAttack(String userId, String attackType, String details) {
        // Log attack
        AttackLog log = new AttackLog(userId, attackType, details, System.currentTimeMillis());
        attackLogs.computeIfAbsent(userId, k -> new ArrayList<>()).add(log);

        // Update count
        int count = attackCounts.getOrDefault(userId, 0) + 1;
        attackCounts.put(userId, count);

        // Check if threshold exceeded
        boolean isActiveAttack = count > THRESHOLD;

        // Clean old logs (keep last 100)
        List<AttackLog> logs = attackLogs.get(userId);
        if (logs != null && logs.size() > 100) {
            attackLogs.put(userId, new ArrayList<>(logs.subList(logs.size() - 100, logs.size())));
        }

        return new AttackDetectionResult(
            isActiveAttack,
            count,
            generateAlert(isActiveAttack, userId, attackType),
            isActiveAttack ? "BLOCK" : "MONITOR"
        );
    }

    private String generateAlert(boolean isActive, String userId, String attackType) {
        if (isActive) {
            return "🚨 ACTIVE ATTACK: User " + userId + " is under " + attackType + " attack!";
        }
        return "⚠️ Suspicious activity detected from user " + userId + " (" + attackType + ")";
    }

    /**
     * Get attack stats
     */
    public Map<String, Object> getAttackStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalAttacks", attackCounts.values().stream().mapToInt(Integer::intValue).sum());
        stats.put("activeUsers", attackCounts.size());
        stats.put("blockedUsers", attackCounts.entrySet().stream()
            .filter(e -> e.getValue() > THRESHOLD)
            .count());
        return stats;
    }

    /**
     * Reset attack count for user
     */
    public void resetAttackCount(String userId) {
        attackCounts.remove(userId);
        attackLogs.remove(userId);
    }

    /**
     * Get attack logs for user
     */
    public List<AttackLog> getAttackLogs(String userId) {
        return attackLogs.getOrDefault(userId, new ArrayList<>());
    }

    public static class AttackLog {
        private final String userId;
        private final String attackType;
        private final String details;
        private final long timestamp;

        public AttackLog(String userId, String attackType, String details, long timestamp) {
            this.userId = userId;
            this.attackType = attackType;
            this.details = details;
            this.timestamp = timestamp;
        }

        public String getUserId() { return userId; }
        public String getAttackType() { return attackType; }
        public String getDetails() { return details; }
        public long getTimestamp() { return timestamp; }
    }

    public static class AttackDetectionResult {
        private final boolean isActiveAttack;
        private final int attackCount;
        private final String alert;
        private final String action;

        public AttackDetectionResult(boolean isActiveAttack, int attackCount, String alert, String action) {
            this.isActiveAttack = isActiveAttack;
            this.attackCount = attackCount;
            this.alert = alert;
            this.action = action;
        }

        public boolean isActiveAttack() { return isActiveAttack; }
        public int getAttackCount() { return attackCount; }
        public String getAlert() { return alert; }
        public String getAction() { return action; }
    }
}