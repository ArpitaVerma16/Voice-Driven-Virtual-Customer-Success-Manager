package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.ml.TicketClassifier;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@lombok.RequiredArgsConstructor
public class SmartRouter {

    private final TicketClassifier classifier;

    private final ComplaintRepository complaintRepository;

    private final UserRepository userRepository;

    // Admin expertise mapping
    private static final Map<String, List<String>> ADMIN_EXPERTISE = new HashMap<>();

    static {
        ADMIN_EXPERTISE.put("admin@example.com", Arrays.asList("NOISE", "MAINTENANCE", "SECURITY"));
        ADMIN_EXPERTISE.put("security@example.com", Arrays.asList("SECURITY", "PARKING"));
        ADMIN_EXPERTISE.put("maintenance@example.com", Arrays.asList("MAINTENANCE", "UTILITIES"));
    }

    /**
     * Classify and route complaint
     */
    public RoutingResult classifyAndRoute(Complaint complaint) {
        TicketClassifier.ClassificationResult result = classifier.classify(complaint.getDescription());

        // Calculate urgency score
        int urgency = calculateUrgency(complaint.getDescription(), result.getCategory());

        // Find best admin
        User bestAdmin = findBestAdmin(result.getCategory());

        // Check for duplicates
        List<Complaint> duplicates = findDuplicates(complaint);

        // Calculate confidence
        double confidence = result.getConfidence();
        if (!duplicates.isEmpty()) {
            confidence = Math.max(0, confidence - (duplicates.size() * 0.05));
        }

        return new RoutingResult(
            complaint.getId(),
            result.getCategory(),
            confidence,
            urgency,
            bestAdmin != null ? bestAdmin.getId() : null,
            bestAdmin != null ? bestAdmin.getName() : "Unassigned",
            duplicates.size(),
            generateRecommendation(result.getCategory(), urgency)
        );
    }

    private int calculateUrgency(String description, String category) {
        String lower = description.toLowerCase();
        int urgency = 50;

        // Base urgency by category
        Map<String, Integer> categoryUrgency = Map.of(
            "SECURITY", 100,
            "MAINTENANCE", 75,
            "UTILITIES", 70,
            "NOISE", 55,
            "CLEANLINESS", 40,
            "PARKING", 35,
            "OTHER", 25
        );
        urgency = categoryUrgency.getOrDefault(category, 50);

        // Keyword boosts
        if (containsAny(lower, "emergency", "urgent", "critical", "danger", "fire")) urgency += 20;
        if (containsAny(lower, "water", "leak", "flood", "power", "outage")) urgency += 15;
        if (containsAny(lower, "again", "repeat", "still", "not fixed")) urgency += 10;

        return Math.min(100, urgency);
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }

    private User findBestAdmin(String category) {
        // Find admin with matching expertise
        for (Map.Entry<String, List<String>> entry : ADMIN_EXPERTISE.entrySet()) {
            if (entry.getValue().contains(category)) {
                return userRepository.findByEmail(entry.getKey()).orElse(null);
            }
        }
        // Fallback to first admin
        return userRepository.findByEmail("admin@example.com").orElse(null);
    }

    private List<Complaint> findDuplicates(Complaint complaint) {
        List<Complaint> allComplaints = complaintRepository.findAll();
        return allComplaints.stream()
            .filter(c -> !c.getId().equals(complaint.getId()))
            .filter(c -> c.getStatus() != Complaint.ComplaintStatus.RESOLVED)
            .filter(c -> {
                String desc1 = c.getDescription().toLowerCase();
                String desc2 = complaint.getDescription().toLowerCase();
                String[] words = desc2.split(" ");
                long matches = 0;
                for (String w : words) {
                    if (desc1.contains(w) && w.length() > 3) matches++;
                }
                return matches >= 2;
            })
            .limit(5)
            .collect(Collectors.toList());
    }

    private String generateRecommendation(String category, int urgency) {
        if (urgency >= 80) {
            return "⚠️ CRITICAL: Immediate action required. Escalate to admin and notify resident.";
        } else if (urgency >= 60) {
            return "🔴 HIGH: Priority action needed within 4 hours.";
        } else if (urgency >= 40) {
            return "🟡 MEDIUM: Normal priority. Handle within 24 hours.";
        } else {
            return "🟢 LOW: Standard priority. Handle within 48 hours.";
        }
    }

    public static class RoutingResult {
        private final Long complaintId;
        private final String category;
        private final double confidence;
        private final int urgency;
        private final Long assignedToId;
        private final String assignedToName;
        private final int duplicateCount;
        private final String recommendation;

        public RoutingResult(Long complaintId, String category, double confidence, 
                             int urgency, Long assignedToId, String assignedToName, 
                             int duplicateCount, String recommendation) {
            this.complaintId = complaintId;
            this.category = category;
            this.confidence = confidence;
            this.urgency = urgency;
            this.assignedToId = assignedToId;
            this.assignedToName = assignedToName;
            this.duplicateCount = duplicateCount;
            this.recommendation = recommendation;
        }

        // Getters
        public Long getComplaintId() { return complaintId; }
        public String getCategory() { return category; }
        public double getConfidence() { return confidence; }
        public int getUrgency() { return urgency; }
        public Long getAssignedToId() { return assignedToId; }
        public String getAssignedToName() { return assignedToName; }
        public int getDuplicateCount() { return duplicateCount; }
        public String getRecommendation() { return recommendation; }
    }
}