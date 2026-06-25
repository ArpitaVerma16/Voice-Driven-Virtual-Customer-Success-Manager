package com.vcsm.ai;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class LocalModelTrainer {

    /**
     * Train local model on client data
     */
    public LocalModelResult trainLocalModel(List<double[]> features, List<Double> labels, int epochs) {
        // Simulate local training
        int featureSize = features.isEmpty() ? 0 : features.get(0).length;
        double[] weights = initializeWeights(featureSize);

        for (int epoch = 0; epoch < epochs; epoch++) {
            for (int i = 0; i < features.size(); i++) {
                double[] feature = features.get(i);
                double label = labels.get(i);
                
                // Simple gradient update
                double prediction = predict(feature, weights);
                double error = prediction - label;
                
                for (int j = 0; j < weights.length; j++) {
                    weights[j] -= 0.01 * error * feature[j];
                }
            }
        }

        // Add small noise for privacy
        for (int i = 0; i < weights.length; i++) {
            weights[i] += ThreadLocalRandom.current().nextGaussian() * 0.001;
        }

        return new LocalModelResult(
            weights,
            features.size(),
            epochs,
            calculateAccuracy(features, labels, weights)
        );
    }

    private double[] initializeWeights(int size) {
        double[] weights = new double[size];
        for (int i = 0; i < size; i++) {
            weights[i] = ThreadLocalRandom.current().nextGaussian() * 0.1;
        }
        return weights;
    }

    private double predict(double[] features, double[] weights) {
        double sum = 0;
        for (int i = 0; i < features.length; i++) {
            sum += features[i] * weights[i];
        }
        return 1.0 / (1.0 + Math.exp(-sum)); // Sigmoid
    }

    private double calculateAccuracy(List<double[]> features, List<Double> labels, double[] weights) {
        int correct = 0;
        for (int i = 0; i < features.size(); i++) {
            double prediction = predict(features.get(i), weights);
            double predictedLabel = prediction > 0.5 ? 1.0 : 0.0;
            if (Math.abs(predictedLabel - labels.get(i)) < 0.5) {
                correct++;
            }
        }
        return features.isEmpty() ? 0 : (double) correct / features.size();
    }

    public static class LocalModelResult {
        private final double[] weights;
        private final int dataSize;
        private final int epochs;
        private final double accuracy;

        public LocalModelResult(double[] weights, int dataSize, int epochs, double accuracy) {
            this.weights = weights;
            this.dataSize = dataSize;
            this.epochs = epochs;
            this.accuracy = accuracy;
        }

        public double[] getWeights() { return weights; }
        public int getDataSize() { return dataSize; }
        public int getEpochs() { return epochs; }
        public double getAccuracy() { return accuracy; }
    }
}