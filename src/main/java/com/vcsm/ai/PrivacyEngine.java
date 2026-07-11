package com.vcsm.ai;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class PrivacyEngine {

    public static final double DEFAULT_EPSILON = 0.5;
    public static final double DEFAULT_DELTA = 1e-5;
    private static final double CLIP_BOUND = 1.0;

    /**
     * Apply differential privacy to model updates
     */
    public double[] applyDifferentialPrivacy(double[] weights, double epsilon, double delta) {
        double[] privateWeights = weights.clone();
        
        // Add Laplace noise for differential privacy
        double sensitivity = CLIP_BOUND / weights.length;
        double scale = sensitivity / epsilon;
        
        for (int i = 0; i < privateWeights.length; i++) {
            privateWeights[i] += laplaceNoise(scale);
        }
        
        return privateWeights;
    }

    private double laplaceNoise(double scale) {
        double u = ThreadLocalRandom.current().nextDouble() - 0.5;
        return -scale * Math.signum(u) * Math.log(1 - 2 * Math.abs(u));
    }

    /**
     * Apply gradient clipping
     */
    public double[] clipGradients(double[] gradients, double clipNorm) {
        double norm = calculateNorm(gradients);
        if (norm > clipNorm) {
            double scale = clipNorm / norm;
            for (int i = 0; i < gradients.length; i++) {
                gradients[i] *= scale;
            }
        }
        return gradients;
    }

    private double calculateNorm(double[] vector) {
        double sum = 0;
        for (double v : vector) {
            sum += v * v;
        }
        return Math.sqrt(sum);
    }

    /**
     * Generate privacy budget report
     */
    public PrivacyReport getPrivacyReport(int iterations) {
        double totalEpsilon = iterations * DEFAULT_EPSILON;
        double totalDelta = iterations * DEFAULT_DELTA;

        return new PrivacyReport(
            DEFAULT_EPSILON,
            DEFAULT_DELTA,
            totalEpsilon,
            totalDelta,
            iterations,
            totalEpsilon < 10 ? "Good privacy protection" : "Privacy budget may be exhausted"
        );
    }

    public static class PrivacyReport {
        private final double perIterationEpsilon;
        private final double perIterationDelta;
        private final double totalEpsilon;
        private final double totalDelta;
        private final int iterations;
        private final String recommendation;

        public PrivacyReport(double perIterationEpsilon, double perIterationDelta,
                            double totalEpsilon, double totalDelta, int iterations,
                            String recommendation) {
            this.perIterationEpsilon = perIterationEpsilon;
            this.perIterationDelta = perIterationDelta;
            this.totalEpsilon = totalEpsilon;
            this.totalDelta = totalDelta;
            this.iterations = iterations;
            this.recommendation = recommendation;
        }

        public double getPerIterationEpsilon() { return perIterationEpsilon; }
        public double getPerIterationDelta() { return perIterationDelta; }
        public double getTotalEpsilon() { return totalEpsilon; }
        public double getTotalDelta() { return totalDelta; }
        public int getIterations() { return iterations; }
        public String getRecommendation() { return recommendation; }
    }
}