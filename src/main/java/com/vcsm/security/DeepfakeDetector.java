package com.vcsm.security;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DeepfakeDetector {

    /**
     * Analyze voice sample for deepfake indicators
     */
    public DeepfakeAnalysis analyze(byte[] audioData, String userId) {
        // Extract spectral features (simplified)
        Map<String, Double> features = extractFeatures(audioData);

        // Calculate anomaly score
        double anomalyScore = calculateAnomalyScore(features);

        // Determine if deepfake
        boolean isDeepfake = anomalyScore > 0.7;

        // Generate confidence
        double confidence = 1.0 - anomalyScore;

        return new DeepfakeAnalysis(
            isDeepfake,
            confidence,
            anomalyScore,
            features,
            generateRecommendation(anomalyScore)
        );
    }

    private Map<String, Double> extractFeatures(byte[] audioData) {
        Map<String, Double> features = new HashMap<>();

        // Simulated feature extraction
        // In production, use actual spectral analysis
        features.put("spectral_centroid", 2000 + ThreadLocalRandom.current().nextDouble() * 1000);
        features.put("spectral_bandwidth", 800 + ThreadLocalRandom.current().nextDouble() * 500);
        features.put("spectral_rolloff", 4000 + ThreadLocalRandom.current().nextDouble() * 1000);
        features.put("zero_crossing_rate", 0.02 + ThreadLocalRandom.current().nextDouble() * 0.04);
        features.put("energy", 0.3 + ThreadLocalRandom.current().nextDouble() * 0.4);
        features.put("mfcc_mean", -5 + ThreadLocalRandom.current().nextDouble() * 10);
        features.put("mfcc_std", 1 + ThreadLocalRandom.current().nextDouble() * 3);

        return features;
    }

    private double calculateAnomalyScore(Map<String, Double> features) {
        // Simulated anomaly detection
        // In production, use trained ML model
        double score = 0.0;
        for (double value : features.values()) {
            if (value < 0.1 || value > 0.9) {
                score += 0.2;
            }
        }
        return Math.min(1.0, score + ThreadLocalRandom.current().nextDouble() * 0.2);
    }

    private String generateRecommendation(double anomalyScore) {
        if (anomalyScore > 0.8) {
            return "🚨 HIGH RISK: Voice appears to be deepfake. Block access and notify admin.";
        } else if (anomalyScore > 0.5) {
            return "⚠️ MEDIUM RISK: Suspicious voice detected. Recommend additional verification.";
        } else {
            return "✅ LOW RISK: Voice appears legitimate. Proceed with normal authentication.";
        }
    }

    public static class DeepfakeAnalysis {
        private final boolean isDeepfake;
        private final double confidence;
        private final double anomalyScore;
        private final Map<String, Double> features;
        private final String recommendation;

        public DeepfakeAnalysis(boolean isDeepfake, double confidence, double anomalyScore,
                                Map<String, Double> features, String recommendation) {
            this.isDeepfake = isDeepfake;
            this.confidence = confidence;
            this.anomalyScore = anomalyScore;
            this.features = features;
            this.recommendation = recommendation;
        }

        public boolean isDeepfake() { return isDeepfake; }
        public double getConfidence() { return confidence; }
        public double getAnomalyScore() { return anomalyScore; }
        public Map<String, Double> getFeatures() { return features; }
        public String getRecommendation() { return recommendation; }
    }
}