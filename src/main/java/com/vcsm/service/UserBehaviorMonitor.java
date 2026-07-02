package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@lombok.RequiredArgsConstructor
public class UserBehaviorMonitor {

    private final ComplaintRepository complaintRepository;

    private final UserRepository userRepository;

    // In-memory user behavior tracking
    private final Map<Long, UserBehavior> userBehaviorMap = new ConcurrentHashMap<>();

    /**
     * Track user behavior and detect patterns
     */
    public BehaviorAnalysis analyzeUserBehavior(User user) {
        UserBehavior behavior = getUserBehavior(user);
        List<Complaint> userComplaints = complaintRepository.findByResidentUsernameOrderByCreatedAtDesc(user.getEmail());

        // Update behavior metrics
        updateBehaviorMetrics(behavior, userComplaints);

        // Detect patterns
        List<String> detectedPatterns = detectPatterns(behavior, userComplaints);

        // Calculate risk score
        int riskScore = calculateRiskScore(behavior, userComplaints);

        // Generate recommendations
        List<String> recommendations = generateRecommendations(riskScore, detectedPatterns);

        return new BehaviorAnalysis(
            user.getId(),
            user.getName(),
            riskScore,
            detectedPatterns,
            recommendations,
            behavior.getLastInteraction(),
            behavior.getTotalInteractions()
        );
    }

    private UserBehavior getUserBehavior(User user) {
        return userBehaviorMap.computeIfAbsent(user.getId(), 
            id -> new UserBehavior(user.getId()));
    }

    private void updateBehaviorMetrics(UserBehavior behavior, List<Complaint> complaints) {
        behavior.setTotalInteractions(complaints.size());
        
        if (!complaints.isEmpty()) {
            behavior.setLastInteraction(complaints.get(0).getCreatedAt());
            
            // Count repeated complaints (same category)
            Map<String, Long> categoryCount = complaints.stream()
                .filter(c -> c.getCategory() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getCategory().toString(),
                    Collectors.counting()
                ));
            
            behavior.setCategoryCounts(categoryCount);
            
            // Count unresolved complaints
            long unresolved = complaints.stream()
                .filter(c -> c.getStatus() != Complaint.ComplaintStatus.RESOLVED)
                .count();
            behavior.setUnresolvedCount(unresolved);
            
            // Check for frustration signals
            boolean frustrated = complaints.stream()
                .anyMatch(c -> c.getDescription().toLowerCase()
                    .contains("again") || 
                    c.getDescription().toLowerCase().contains("third time") ||
                    c.getDescription().toLowerCase().contains("still not") ||
                    c.getDescription().toLowerCase().contains("frustrated"));
            behavior.setFrustrated(frustrated);
        }
        
        userBehaviorMap.put(behavior.getUserId(), behavior);
    }

    private List<String> detectPatterns(UserBehavior behavior, List<Complaint> complaints) {
        List<String> patterns = new ArrayList<>();

        if (behavior.isFrustrated()) {
            patterns.add("FRUSTRATION_DETECTED");
        }

        if (behavior.getUnresolvedCount() > 3) {
            patterns.add("MULTIPLE_UNRESOLVED");
        }

        if (behavior.getTotalInteractions() > 5) {
            patterns.add("HIGH_INTERACTION_USER");
        }

        if (behavior.getCategoryCounts() != null) {
            for (Map.Entry<String, Long> entry : behavior.getCategoryCounts().entrySet()) {
                if (entry.getValue() > 2) {
                    patterns.add("REPEATED_" + entry.getKey().toUpperCase());
                }
            }
        }

        // Check for time-based patterns
        if (!complaints.isEmpty()) {
            LocalDateTime first = complaints.get(complaints.size() - 1).getCreatedAt();
            LocalDateTime last = complaints.get(0).getCreatedAt();
            long daysBetween = ChronoUnit.DAYS.between(first, last);
            if (daysBetween < 7 && complaints.size() > 2) {
                patterns.add("HIGH_FREQUENCY");
            }
        }

        return patterns;
    }

    private int calculateRiskScore(UserBehavior behavior, List<Complaint> complaints) {
        int score = 0;

        // Frustration adds high risk
        if (behavior.isFrustrated()) score += 30;

        // Multiple unresolved complaints
        score += Math.min(behavior.getUnresolvedCount() * 10, 30);

        // High interaction frequency
        if (behavior.getTotalInteractions() > 5) score += 10;

        // Repeated categories
        if (behavior.getCategoryCounts() != null) {
            for (long count : behavior.getCategoryCounts().values()) {
                if (count > 2) score += 5;
            }
        }

        return Math.min(100, score);
    }

    private List<String> generateRecommendations(int riskScore, List<String> patterns) {
        List<String> recommendations = new ArrayList<>();

        if (riskScore >= 70) {
            recommendations.add("🚨 HIGH RISK: Immediate proactive outreach recommended.");
            recommendations.add("📞 Offer escalation to senior support.");
        } else if (riskScore >= 40) {
            recommendations.add("⚠️ MEDIUM RISK: Send personalized follow-up.");
            recommendations.add("📧 Offer additional assistance via email.");
        } else if (riskScore >= 20) {
            recommendations.add("ℹ️ LOW RISK: Monitor user activity.");
            recommendations.add("📱 Send occasional engagement messages.");
        } else {
            recommendations.add("✅ User is satisfied. Maintain regular engagement.");
        }

        if (patterns.contains("FRUSTRATION_DETECTED")) {
            recommendations.add("🤝 Acknowledge frustration and offer direct support.");
        }

        if (patterns.contains("MULTIPLE_UNRESOLVED")) {
            recommendations.add("🔍 Review all unresolved complaints for patterns.");
        }

        if (patterns.contains("HIGH_FREQUENCY")) {
            recommendations.add("📊 User is highly active. Schedule a check-in call.");
        }

        return recommendations;
    }

    public static class UserBehavior {
        private final Long userId;
        private int totalInteractions;
        private LocalDateTime lastInteraction;
        private boolean frustrated;
        private long unresolvedCount;
        private Map<String, Long> categoryCounts;

        public UserBehavior(Long userId) {
            this.userId = userId;
            this.totalInteractions = 0;
            this.unresolvedCount = 0;
            this.frustrated = false;
            this.categoryCounts = new HashMap<>();
        }

        // Getters and Setters
        public Long getUserId() { return userId; }
        public int getTotalInteractions() { return totalInteractions; }
        public void setTotalInteractions(int totalInteractions) { this.totalInteractions = totalInteractions; }
        public LocalDateTime getLastInteraction() { return lastInteraction; }
        public void setLastInteraction(LocalDateTime lastInteraction) { this.lastInteraction = lastInteraction; }
        public boolean isFrustrated() { return frustrated; }
        public void setFrustrated(boolean frustrated) { this.frustrated = frustrated; }
        public long getUnresolvedCount() { return unresolvedCount; }
        public void setUnresolvedCount(long unresolvedCount) { this.unresolvedCount = unresolvedCount; }
        public Map<String, Long> getCategoryCounts() { return categoryCounts; }
        public void setCategoryCounts(Map<String, Long> categoryCounts) { this.categoryCounts = categoryCounts; }
    }

    public static class BehaviorAnalysis {
        private final Long userId;
        private final String userName;
        private final int riskScore;
        private final List<String> detectedPatterns;
        private final List<String> recommendations;
        private final LocalDateTime lastInteraction;
        private final int totalInteractions;

        public BehaviorAnalysis(Long userId, String userName, int riskScore, 
                                List<String> detectedPatterns, List<String> recommendations,
                                LocalDateTime lastInteraction, int totalInteractions) {
            this.userId = userId;
            this.userName = userName;
            this.riskScore = riskScore;
            this.detectedPatterns = detectedPatterns;
            this.recommendations = recommendations;
            this.lastInteraction = lastInteraction;
            this.totalInteractions = totalInteractions;
        }

        // Getters
        public Long getUserId() { return userId; }
        public String getUserName() { return userName; }
        public int getRiskScore() { return riskScore; }
        public List<String> getDetectedPatterns() { return detectedPatterns; }
        public List<String> getRecommendations() { return recommendations; }
        public LocalDateTime getLastInteraction() { return lastInteraction; }
        public int getTotalInteractions() { return totalInteractions; }
    }
}
