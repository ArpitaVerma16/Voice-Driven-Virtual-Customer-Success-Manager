package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.repository.ComplaintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Profile("dev")
@Service
@lombok.RequiredArgsConstructor
public class DriftDetector {

    private final ComplaintRepository complaintRepository;

    private Map<String, List<Double>> featureDistributions = new HashMap<>();

    /**
     * Detect data drift in incoming complaints
     */
    public DriftResult detectDrift() {
        List<Complaint> recentComplaints = getRecentComplaints();
        List<Complaint> baselineComplaints = getBaselineComplaints();

        if (recentComplaints.isEmpty() || baselineComplaints.isEmpty()) {
            return new DriftResult(false, "Insufficient data", 0.0);
        }

        // Calculate drift score
        double driftScore = calculateDriftScore(recentComplaints, baselineComplaints);
        boolean hasDrift = driftScore > 0.3; // 30% drift threshold

        return new DriftResult(hasDrift, driftScore > 0.3 ? "Data drift detected" : "No significant drift", driftScore);
    }

    private List<Complaint> getRecentComplaints() {
        return complaintRepository.findAll().stream()
            .filter(c -> c.getCreatedAt() != null)
            .filter(c -> c.getCreatedAt().isAfter(LocalDateTime.now().minusDays(7)))
            .limit(100)
            .collect(Collectors.toList());
    }

    private List<Complaint> getBaselineComplaints() {
        return complaintRepository.findAll().stream()
            .filter(c -> c.getCreatedAt() != null)
            .filter(c -> c.getCreatedAt().isBefore(LocalDateTime.now().minusDays(7)))
            .limit(200)
            .collect(Collectors.toList());
    }

    private double calculateDriftScore(List<Complaint> recent, List<Complaint> baseline) {
        // Category distribution comparison
        Map<String, Long> recentCategories = recent.stream()
            .filter(c -> c.getCategory() != null)
            .collect(Collectors.groupingBy(c -> c.getCategory().toString(), Collectors.counting()));

        Map<String, Long> baselineCategories = baseline.stream()
            .filter(c -> c.getCategory() != null)
            .collect(Collectors.groupingBy(c -> c.getCategory().toString(), Collectors.counting()));

        double totalRecent = recentCategories.values().stream().mapToLong(Long::longValue).sum();
        double totalBaseline = baselineCategories.values().stream().mapToLong(Long::longValue).sum();

        Set<String> allCategories = new HashSet<>();
        allCategories.addAll(recentCategories.keySet());
        allCategories.addAll(baselineCategories.keySet());

        double totalDiff = 0;
        for (String category : allCategories) {
            double recentRatio = totalRecent > 0 ? recentCategories.getOrDefault(category, 0L) / totalRecent : 0;
            double baselineRatio = totalBaseline > 0 ? baselineCategories.getOrDefault(category, 0L) / totalBaseline : 0;
            totalDiff += Math.abs(recentRatio - baselineRatio);
        }

        return totalDiff / allCategories.size();
    }

    public static class DriftResult {
        private final boolean hasDrift;
        private final String message;
        private final double driftScore;

        public DriftResult(boolean hasDrift, String message, double driftScore) {
            this.hasDrift = hasDrift;
            this.message = message;
            this.driftScore = driftScore;
        }

        public boolean isHasDrift() { return hasDrift; }
        public String getMessage() { return message; }
        public double getDriftScore() { return driftScore; }
    }
}
