package com.vcsm.service;

import com.vcsm.model.Complaint;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared utility methods extracted from SmartRouter and TriageService
 * to eliminate code duplication.
 */
public final class ComplaintRoutingUtils {

    private ComplaintRoutingUtils() {}

    public static boolean containsAny(String text, String... keywords) {
        String lower = text.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsAny(String text, Set<String> keywords) {
        String lower = text.toLowerCase();
        return keywords.stream().anyMatch(k -> lower.contains(k.toLowerCase()));
    }

    public static int calculateUrgency(String category, String description, Map<String, Integer> categoryUrgency) {
        int base = categoryUrgency.getOrDefault(category.toLowerCase(), 2);
        if (containsAny(description, "urgent", "emergency", "critical", "asap")) return 5;
        if (containsAny(description, "important", "escalate", "serious")) return Math.max(base, 4);
        return base;
    }

    public static List<Complaint> findSimilarComplaints(
            String category, String description, List<Complaint> allComplaints, String... excludeKeywords) {
        return allComplaints.stream()
                .filter(c -> c.getCategory() != null && c.getCategory().equalsIgnoreCase(category))
                .filter(c -> c.getDescription() != null && containsAny(c.getDescription(), excludeKeywords))
                .collect(Collectors.toList());
    }

    public static boolean isSimilarDescription(String desc1, String desc2, int minOverlap) {
        if (desc1 == null || desc2 == null) return false;
        Set<String> words1 = Set.of(desc1.toLowerCase().split("\\s+"));
        Set<String> words2 = Set.of(desc2.toLowerCase().split("\\s+"));
        long overlap = words1.stream().filter(words2::contains).count();
        return overlap >= minOverlap;
    }
}
