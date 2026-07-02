package com.vcsm.service;

import com.vcsm.model.FeatureUsage;
import com.vcsm.repository.FeatureUsageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@lombok.RequiredArgsConstructor
public class FeatureAnalyzer {

    private final FeatureUsageRepository featureUsageRepository;

    /**
     * Analyze feature usage and generate recommendations
     */
    public FeatureAnalysis analyzeFeatures() {
        List<FeatureUsage> allUsages = featureUsageRepository.findAll();
        Map<String, FeatureStats> statsMap = new HashMap<>();

        for (FeatureUsage usage : allUsages) {
            String feature = usage.getFeatureName();
            statsMap.computeIfAbsent(feature, k -> new FeatureStats(feature))
                .addUsage(usage);
        }

        return new FeatureAnalysis(statsMap);
    }

    /**
     * Identify features to evolve (enable/disable)
     */
    public List<String> getEvolutionRecommendations() {
    FeatureAnalysis analysis = analyzeFeatures();
    return analysis.getRecommendations(); 
}

    /**
     * Calculate feature health score
     */
    public double calculateHealthScore(String featureName) {
        List<FeatureUsage> usages = featureUsageRepository.findByFeatureName(featureName);
        if (usages.isEmpty()) return 0.0;

        double avgUsage = usages.stream().mapToInt(FeatureUsage::getUsageCount).average().orElse(0);
        double avgSuccess = usages.stream().mapToDouble(FeatureUsage::getSuccessRate).average().orElse(0);
        double avgRating = usages.stream().mapToDouble(FeatureUsage::getUserRating).average().orElse(0);
        long activeUsers = usages.stream().filter(FeatureUsage::isActive).count();

        // Weighted score
        double score = (avgUsage * 0.3) + (avgSuccess * 0.3) + (avgRating * 0.3) + (activeUsers * 0.1);
        return Math.min(100, score);
    }

    public static class FeatureStats {
        private final String featureName;
        private int totalUsage = 0;
        private double totalSuccessRate = 0;
        private double totalRating = 0;
        private int usageCount = 0;
        private boolean active = true;
        private LocalDateTime lastUsed;

        public FeatureStats(String featureName) {
            this.featureName = featureName;
        }

        public void addUsage(FeatureUsage usage) {
            totalUsage += usage.getUsageCount();
            totalSuccessRate += usage.getSuccessRate();
            totalRating += usage.getUserRating();
            usageCount++;
            if (usage.getLastUsed() != null) {
                lastUsed = usage.getLastUsed();
            }
        }

        public String getFeatureName() { return featureName; }
        public int getTotalUsage() { return totalUsage; }
        public double getAvgSuccessRate() { return usageCount > 0 ? totalSuccessRate / usageCount : 0; }
        public double getAvgRating() { return usageCount > 0 ? totalRating / usageCount : 0; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public LocalDateTime getLastUsed() { return lastUsed; }
    }

    public static class FeatureAnalysis {
        private final Map<String, FeatureStats> stats;
        private final List<String> recommendations;

        public FeatureAnalysis(Map<String, FeatureStats> stats) {
            this.stats = stats;
            this.recommendations = generateRecommendations();
        }

        private List<String> generateRecommendations() {
            List<String> recs = new ArrayList<>();

            for (Map.Entry<String, FeatureStats> entry : stats.entrySet()) {
                String feature = entry.getKey();
                FeatureStats stat = entry.getValue();

                // Check if feature is stale
                if (stat.getTotalUsage() < 10) {
                    recs.add("⚠️ Feature '" + feature + "' has low usage. Consider disabling or improving.");
                }

                // Check if feature has poor ratings
                if (stat.getAvgRating() < 2.5) {
                    recs.add("🔴 Feature '" + feature + "' has poor user rating (" + 
                        String.format("%.1f", stat.getAvgRating()) + "/5). Consider improving.");
                }

                // Check if feature has high success
                if (stat.getAvgSuccessRate() > 90) {
                    recs.add("✅ Feature '" + feature + "' has high success rate (" + 
                        String.format("%.1f", stat.getAvgSuccessRate()) + "%). Promote this feature!");
                }
            }

            return recs;
        }

        public Map<String, FeatureStats> getStats() { return stats; }
        public List<String> getRecommendations() { return recommendations; }
    }
}
