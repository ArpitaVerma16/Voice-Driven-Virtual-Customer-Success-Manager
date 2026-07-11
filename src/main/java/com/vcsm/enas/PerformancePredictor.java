package com.vcsm.enas;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PerformancePredictor {

    private final Map<String, Double> layerSizePerformance = new ConcurrentHashMap<>();
    private final Map<String, Double> activationPerformance = new ConcurrentHashMap<>();

    public PerformancePredictor() {
        initializePerformanceData();
    }

    private void initializePerformanceData() {
        layerSizePerformance.put("small", 0.6);
        layerSizePerformance.put("medium", 0.75);
        layerSizePerformance.put("large", 0.85);
        layerSizePerformance.put("huge", 0.9);

        activationPerformance.put("RELU", 0.7);
        activationPerformance.put("TANH", 0.65);
        activationPerformance.put("SIGMOID", 0.6);
        activationPerformance.put("SOFTMAX", 0.55);
    }

    /**
     * Predict architecture performance
     */
    public PredictedPerformance predict(NeuralArchitecture arch) {
        double predictedAccuracy = predictAccuracy(arch);
        double predictedLatency = predictLatency(arch);
        double predictedMemory = predictMemory(arch);
        double predictedEnergy = predictEnergy(arch);

        return new PredictedPerformance(
            predictedAccuracy,
            predictedLatency,
            predictedMemory,
            predictedEnergy,
            arch
        );
    }

    private double predictAccuracy(NeuralArchitecture arch) {
        int numLayers = arch.getLayerSizes().size();
        int maxSize = arch.getLayerSizes().stream().mapToInt(Integer::intValue).max().orElse(0);
        
        double accuracy = 0.5;
        accuracy += Math.min(0.3, numLayers * 0.04);
        accuracy += Math.min(0.15, maxSize / 200.0);
        
        // Activation function impact
        for (String activation : arch.getActivationFunctions()) {
            accuracy += activationPerformance.getOrDefault(activation, 0.0) * 0.02;
        }
        
        return Math.min(0.98, accuracy + (Math.random() * 0.05) - 0.025);
    }

    private double predictLatency(NeuralArchitecture arch) {
        int totalParams = arch.getParameters();
        return 5 + (totalParams / 1000.0) + (arch.getLayerSizes().size() * 2);
    }

    private double predictMemory(NeuralArchitecture arch) {
        int totalParams = arch.getParameters();
        return 10 + (totalParams / 500.0);
    }

    private double predictEnergy(NeuralArchitecture arch) {
        int totalParams = arch.getParameters();
        return 5 + (totalParams / 2000.0);
    }

    /**
     * Compare two architectures
     */
    public ComparisonResult compare(NeuralArchitecture arch1, NeuralArchitecture arch2) {
        PredictedPerformance perf1 = predict(arch1);
        PredictedPerformance perf2 = predict(arch2);

        return new ComparisonResult(
            perf1,
            perf2,
            perf1.getPredictedAccuracy() > perf2.getPredictedAccuracy(),
            perf1.getPredictedLatency() < perf2.getPredictedLatency(),
            perf1.getPredictedMemory() < perf2.getPredictedMemory()
        );
    }

    public static class PredictedPerformance {
        private final double predictedAccuracy;
        private final double predictedLatency;
        private final double predictedMemory;
        private final double predictedEnergy;
        private final NeuralArchitecture architecture;

        public PredictedPerformance(double accuracy, double latency, double memory, 
                                   double energy, NeuralArchitecture arch) {
            this.predictedAccuracy = accuracy;
            this.predictedLatency = latency;
            this.predictedMemory = memory;
            this.predictedEnergy = energy;
            this.architecture = arch;
        }

        public double getPredictedAccuracy() { return predictedAccuracy; }
        public double getPredictedLatency() { return predictedLatency; }
        public double getPredictedMemory() { return predictedMemory; }
        public double getPredictedEnergy() { return predictedEnergy; }
        public NeuralArchitecture getArchitecture() { return architecture; }
    }

    public static class ComparisonResult {
        private final PredictedPerformance performance1;
        private final PredictedPerformance performance2;
        private final boolean accuracyWinner1;
        private final boolean latencyWinner1;
        private final boolean memoryWinner1;

        public ComparisonResult(PredictedPerformance p1, PredictedPerformance p2,
                               boolean accuracyW1, boolean latencyW1, boolean memoryW1) {
            this.performance1 = p1;
            this.performance2 = p2;
            this.accuracyWinner1 = accuracyW1;
            this.latencyWinner1 = latencyW1;
            this.memoryWinner1 = memoryW1;
        }

        public PredictedPerformance getPerformance1() { return performance1; }
        public PredictedPerformance getPerformance2() { return performance2; }
        public boolean isAccuracyWinner1() { return accuracyWinner1; }
        public boolean isLatencyWinner1() { return latencyWinner1; }
        public boolean isMemoryWinner1() { return memoryWinner1; }
    }
}
