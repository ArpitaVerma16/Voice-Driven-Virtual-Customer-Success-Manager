package com.vcsm.security;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RobustnessVerifier {

    /**
     * Verify model robustness
     */
    public RobustnessReport verifyRobustness(String modelName) {
        // Simulate robustness testing
        List<RobustnessTest> tests = new ArrayList<>();

        tests.add(new RobustnessTest("Test 1: Input Perturbation", 85, "PASSED"));
        tests.add(new RobustnessTest("Test 2: Adversarial Examples", 72, "WARNING"));
        tests.add(new RobustnessTest("Test 3: Noise Injection", 90, "PASSED"));
        tests.add(new RobustnessTest("Test 4: Out-of-Distribution", 78, "WARNING"));

        double overallScore = tests.stream()
            .mapToInt(RobustnessTest::getScore)
            .average()
            .orElse(0);

        boolean isRobust = overallScore > 75;

        return new RobustnessReport(
            modelName,
            overallScore,
            isRobust,
            tests,
            generateRecommendation(overallScore)
        );
    }

    private String generateRecommendation(double score) {
        if (score > 85) {
            return "✅ Model is robust. No action needed.";
        } else if (score > 70) {
            return "⚠️ Model needs improvement. Consider adversarial training.";
        } else {
            return "🔴 Model is vulnerable. Immediate retraining recommended.";
        }
    }

    public static class RobustnessTest {
        private final String name;
        private final int score;
        private final String status;

        public RobustnessTest(String name, int score, String status) {
            this.name = name;
            this.score = score;
            this.status = status;
        }

        public String getName() { return name; }
        public int getScore() { return score; }
        public String getStatus() { return status; }
    }

    public static class RobustnessReport {
        private final String modelName;
        private final double overallScore;
        private final boolean isRobust;
        private final List<RobustnessTest> tests;
        private final String recommendation;

        public RobustnessReport(String modelName, double overallScore, boolean isRobust,
                               List<RobustnessTest> tests, String recommendation) {
            this.modelName = modelName;
            this.overallScore = overallScore;
            this.isRobust = isRobust;
            this.tests = tests;
            this.recommendation = recommendation;
        }

        public String getModelName() { return modelName; }
        public double getOverallScore() { return overallScore; }
        public boolean isRobust() { return isRobust; }
        public List<RobustnessTest> getTests() { return tests; }
        public String getRecommendation() { return recommendation; }
    }
}