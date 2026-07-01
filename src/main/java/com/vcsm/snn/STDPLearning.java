package com.vcsm.snn;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class STDPLearning {

    private static final double A_PLUS = 0.01;
    private static final double A_MINUS = 0.012;
    private static final double TAU_PLUS = 20.0;
    private static final double TAU_MINUS = 20.0;

    private final Map<String, STDPConnection> connections = new ConcurrentHashMap<>();

    /**
     * Create a connection between neurons
     */
    public STDPConnection createConnection(String preNeuron, String postNeuron, double initialWeight) {
        STDPConnection connection = new STDPConnection(preNeuron, postNeuron, initialWeight);
        connections.put(preNeuron + "->" + postNeuron, connection);
        return connection;
    }

    /**
     * Update weights based on spike timing
     */
    public void updateWeights(long preSpikeTime, long postSpikeTime, double dt) {
        double deltaT = preSpikeTime - postSpikeTime;

        for (STDPConnection connection : connections.values()) {
            double weight = connection.getWeight();

            if (deltaT > 0) {
                // Pre before post - LTP
                weight += A_PLUS * Math.exp(-deltaT / TAU_PLUS) * dt;
            } else if (deltaT < 0) {
                // Post before pre - LTD
                weight -= A_MINUS * Math.exp(deltaT / TAU_MINUS) * dt;
            }

            // Clip weights
            weight = Math.max(0.0, Math.min(1.0, weight));
            connection.setWeight(weight);
        }
    }

    /**
     * Get connection weight
     */
    public double getConnectionWeight(String preNeuron, String postNeuron) {
        STDPConnection connection = connections.get(preNeuron + "->" + postNeuron);
        return connection != null ? connection.getWeight() : 0.0;
    }

    /**
     * Get all connections
     */
    public List<STDPConnection> getAllConnections() {
        return new ArrayList<>(connections.values());
    }

    /**
     * Reset all connections
     */
    public void resetAllConnections() {
        for (STDPConnection connection : connections.values()) {
            connection.setWeight(0.5);
        }
    }

    public static class STDPConnection {
        private final String preNeuron;
        private final String postNeuron;
        private double weight;
        private long lastUpdate;

        public STDPConnection(String preNeuron, String postNeuron, double initialWeight) {
            this.preNeuron = preNeuron;
            this.postNeuron = postNeuron;
            this.weight = initialWeight;
            this.lastUpdate = System.currentTimeMillis();
        }

        public String getPreNeuron() { return preNeuron; }
        public String getPostNeuron() { return postNeuron; }
        public double getWeight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        public long getLastUpdate() { return lastUpdate; }
        public void setLastUpdate(long lastUpdate) { this.lastUpdate = lastUpdate; }
    }
}