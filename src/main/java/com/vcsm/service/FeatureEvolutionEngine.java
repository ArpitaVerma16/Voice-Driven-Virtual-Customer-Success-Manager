package com.vcsm.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Profile("dev")
@Service
@lombok.RequiredArgsConstructor
public class FeatureEvolutionEngine {

    private static final Logger log = LoggerFactory.getLogger(FeatureEvolutionEngine.class);

    private final FeatureAnalyzer featureAnalyzer;

    private final ABTestingService abTestingService;

    private final Map<String, Double> featureScores = new ConcurrentHashMap<>();

    /**
     * Run evolution cycle
     */
    @Scheduled(cron = "0 0 0 * * MON") // Weekly
    public void runEvolutionCycle() {
        log.info("🧬 Starting feature evolution cycle...");

        // 1. Analyze features
        FeatureAnalyzer.FeatureAnalysis analysis = featureAnalyzer.analyzeFeatures();

        // 2. Calculate scores
        for (Map.Entry<String, FeatureAnalyzer.FeatureStats> entry : analysis.getStats().entrySet()) {
            String feature = entry.getKey();
            FeatureAnalyzer.FeatureStats stats = entry.getValue();
            
            // Calculate score based on usage, success, ratings
            double score = (stats.getTotalUsage() * 0.3) + 
                           (stats.getAvgSuccessRate() * 0.4) + 
                           (stats.getAvgRating() * 0.3);
            featureScores.put(feature, score);
        }

        // 3. Generate recommendations
        List<String> recommendations = analysis.getRecommendations();

        // 4. Apply automatic actions
        for (String rec : recommendations) {
            autoAction(rec);
        }

        log.info("✅ Evolution cycle completed");
    }

    private void autoAction(String recommendation) {
        if (recommendation.contains("low usage")) {
            String feature = extractFeatureName(recommendation);
            if (feature != null) {
                log.info("📉 Feature '" + feature + "' will be deprioritized");
                // Disable feature in UI
            }
        } else if (recommendation.contains("high success")) {
            String feature = extractFeatureName(recommendation);
            if (feature != null) {
                log.info("📈 Feature '" + feature + "' will be promoted");
                // Promote feature in UI
            }
        }
    }

    private String extractFeatureName(String recommendation) {
        int start = recommendation.indexOf("'") + 1;
        int end = recommendation.indexOf("'", start);
        return (start > 0 && end > start) ? recommendation.substring(start, end) : null;
    }

    /**
     * Get current feature scores
     */
    public Map<String, Double> getFeatureScores() {
        return featureScores;
    }

    /**
     * Get top performing features
     */
    public List<String> getTopFeatures(int limit) {
        return featureScores.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Get features needing improvement
     */
    public List<String> getStaleFeatures() {
        return featureScores.entrySet().stream()
            .filter(e -> e.getValue() < 30)
            .map(Map.Entry::getKey)
            .toList();
    }
}
