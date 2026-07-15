package com.vcsm.quantum;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class QuantumSecureAggregation {

    private final Map<String, QuantumClientUpdate> clientUpdates = new ConcurrentHashMap<>();
    private final Map<String, String> quantumKeys = new ConcurrentHashMap<>();

    /**
     * Generate quantum-secure key for client
     */
    public String generateQuantumKey(String clientId) {
        String key = "QK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        quantumKeys.put(clientId, key);
        return key;
    }

    /**
     * Encrypt update with quantum key
     */
    public double[] quantumEncrypt(double[] weights, String clientId) {
        String key = quantumKeys.get(clientId);
        if (key == null) {
            throw new CustomDomainException("No quantum key found for client: " + clientId);
        }

        double[] encrypted = weights.clone();
        for (int i = 0; i < encrypted.length; i++) {
            // Simulate quantum encryption
            encrypted[i] = encrypted[i] + (key.hashCode() % 1000) * 0.001;
        }
        return encrypted;
    }

    /**
     * Decrypt update with quantum key
     */
    public double[] quantumDecrypt(double[] encryptedWeights, String clientId) {
        String key = quantumKeys.get(clientId);
        if (key == null) {
            throw new CustomDomainException("No quantum key found for client: " + clientId);
        }

        double[] decrypted = encryptedWeights.clone();
        for (int i = 0; i < decrypted.length; i++) {
            decrypted[i] = decrypted[i] - (key.hashCode() % 1000) * 0.001;
        }
        return decrypted;
    }

    /**
     * Add client update
     */
    public void addClientUpdate(String clientId, double[] weights, int dataSize) {
        QuantumClientUpdate update = new QuantumClientUpdate(clientId, weights, dataSize, System.currentTimeMillis());
        clientUpdates.put(clientId, update);
    }

    /**
     * Quantum aggregate all updates
     */
    public QuantumAggregatedResult quantumAggregate() {
        List<QuantumClientUpdate> updates = new ArrayList<>(clientUpdates.values());
        if (updates.isEmpty()) {
            return new QuantumAggregatedResult(new double[0], 0, "No updates to aggregate");
        }

        int modelSize = updates.get(0).getWeights().length;
        double[] aggregatedWeights = new double[modelSize];
        int totalDataSize = 0;

        // Quantum superposition aggregation
        for (QuantumClientUpdate update : updates) {
            double[] weights = update.getWeights();
            int dataSize = update.getDataSize();
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

        // Add quantum noise for privacy
        for (int i = 0; i < aggregatedWeights.length; i++) {
            aggregatedWeights[i] += ThreadLocalRandom.current().nextGaussian() * 0.001;
        }

        int clientCount = updates.size();
        clientUpdates.clear();

        return new QuantumAggregatedResult(
            aggregatedWeights,
            clientCount,
            "Quantum aggregation successful with " + clientCount + " clients"
        );
    }

    public static class QuantumClientUpdate {
        private final String clientId;
        private final double[] weights;
        private final int dataSize;
        private final long timestamp;

        public QuantumClientUpdate(String clientId, double[] weights, int dataSize, long timestamp) {
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

    public static class QuantumAggregatedResult {
        private final double[] weights;
        private final int clientCount;
        private final String message;

        public QuantumAggregatedResult(double[] weights, int clientCount, String message) {
            this.weights = weights;
            this.clientCount = clientCount;
            this.message = message;
        }

        public double[] getWeights() { return weights; }
        public int getClientCount() { return clientCount; }
        public String getMessage() { return message; }
    }
}