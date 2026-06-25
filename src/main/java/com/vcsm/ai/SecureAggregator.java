package com.vcsm.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SecureAggregator {

    @Autowired
    private PrivacyEngine privacyEngine;

    private final Map<String, LocalModel> localModels = new ConcurrentHashMap<>();

    /**
     * Add a local model update
     */
    public void addLocalUpdate(String clientId, double[] weights, int dataSize) {
        LocalModel model = new LocalModel(clientId, weights, dataSize, System.currentTimeMillis());
        localModels.put(clientId, model);
    }

    /**
     * Aggregate all local models using secure aggregation
     */
    public AggregatedModel aggregate(String sessionId) {
        List<LocalModel> models = new ArrayList<>(localModels.values());
        if (models.isEmpty()) {
            return new AggregatedModel(new double[0], 0, "No models to aggregate");
        }

        // Find common size
        int modelSize = models.get(0).getWeights().length;
        double[] aggregatedWeights = new double[modelSize];
        int totalDataSize = 0;

        // Sum weights (weighted by data size)
        for (LocalModel model : models) {
            double[] weights = model.getWeights();
            int dataSize = model.getDataSize();
            totalDataSize += dataSize;

            for (int i = 0; i < modelSize; i++) {
                if (i < weights.length) {
                    aggregatedWeights[i] += weights[i] * dataSize;
                }
            }
        }

        // Normalize
        if (totalDataSize > 0) {
            for (int i = 0; i < aggregatedWeights.length; i++) {
                aggregatedWeights[i] /= totalDataSize;
            }
        }

        // Apply differential privacy
        double[] privateWeights = privacyEngine.applyDifferentialPrivacy(
            aggregatedWeights,
            PrivacyEngine.DEFAULT_EPSILON,
            PrivacyEngine.DEFAULT_DELTA
        );

        // Clear local models after aggregation
        localModels.clear();

        return new AggregatedModel(
            privateWeights,
            models.size(),
            "Aggregation successful with " + models.size() + " clients"
        );
    }

    /**
     * Get number of participating clients
     */
    public int getParticipantCount() {
        return localModels.size();
    }

    public static class LocalModel {
        private final String clientId;
        private final double[] weights;
        private final int dataSize;
        private final long timestamp;

        public LocalModel(String clientId, double[] weights, int dataSize, long timestamp) {
            this.clientId = clientId;
            this.weights = weights;
            this.dataSize = dataSize;
            this.timestamp = timestamp;
        }

        public String getClientId() { return clientId; }
        public double[] getWeights() { return weights; }
        public int getDataSize() { return dataSize; }
        public long getTimestamp() { return timestamp; }
    }

    public static class AggregatedModel {
        private final double[] weights;
        private final int clientCount;
        private final String message;

        public AggregatedModel(double[] weights, int clientCount, String message) {
            this.weights = weights;
            this.clientCount = clientCount;
            this.message = message;
        }

        public double[] getWeights() { return weights; }
        public int getClientCount() { return clientCount; }
        public String getMessage() { return message; }
    }
}