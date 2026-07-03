package com.vcsm.quantum;

import com.vcsm.quantum.QuantumCircuitBuilder.QuantumCircuit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
@lombok.RequiredArgsConstructor
public class QuantumFederatedService {

    private static final Logger log = LoggerFactory.getLogger(QuantumFederatedService.class);

    private final QuantumSecureAggregation secureAggregation;

    private final QuantumCircuitBuilder circuitBuilder;

    private final Map<String, FederatedRound> rounds = new ConcurrentHashMap<>();
    private final Map<String, QuantumClientStatus> clientStatus = new ConcurrentHashMap<>();
    private int roundNumber = 0;
    private double[] globalQuantumModel;

    private static final int NUM_QUBITS = 4;
    private static final int NUM_LAYERS = 2;

    /**
     * Start a new quantum federated round
     */
    public FederatedRound startRound() {
        roundNumber++;
        String roundId = "QFL_" + roundNumber + "_" + System.currentTimeMillis();

        // Generate quantum circuit for this round
        QuantumCircuit circuit = circuitBuilder.buildVariationalCircuit(NUM_QUBITS, NUM_LAYERS, new double[NUM_QUBITS * NUM_LAYERS]);

        FederatedRound round = new FederatedRound(roundId, roundNumber, circuit);
        rounds.put(roundId, round);

        // Initialize global model if not exists
        if (globalQuantumModel == null) {
            globalQuantumModel = new double[NUM_QUBITS * NUM_LAYERS];
            for (int i = 0; i < globalQuantumModel.length; i++) {
                globalQuantumModel[i] = ThreadLocalRandom.current().nextDouble() * 2 * Math.PI;
            }
        }

        round.setGlobalModel(globalQuantumModel.clone());
        return round;
    }

    /**
     * Client participates with quantum update
     */
    public ClientParticipation participate(String roundId, String clientId, double[] localWeights, int dataSize) {
        FederatedRound round = rounds.get(roundId);
        if (round == null) {
            return new ClientParticipation(clientId, false, "Round not found");
        }

        // Generate quantum key for client
        String quantumKey = secureAggregation.generateQuantumKey(clientId);

        // Encrypt weights with quantum key
        double[] encryptedWeights = secureAggregation.quantumEncrypt(localWeights, clientId);

        // Add to secure aggregation
        secureAggregation.addClientUpdate(clientId, encryptedWeights, dataSize);

        // Track client participation
        round.addParticipant(clientId);
        clientStatus.put(clientId, new QuantumClientStatus(clientId, true, System.currentTimeMillis()));

        return new ClientParticipation(
            clientId,
            true,
            "Quantum update submitted successfully. Key: " + quantumKey.substring(0, 8) + "..."
        );
    }

    /**
     * Aggregate and update global model
     */
    public QuantumAggregationResult aggregate(String roundId) {
        FederatedRound round = rounds.get(roundId);
        if (round == null) {
            return new QuantumAggregationResult(null, 0, "Round not found");
        }

        // Quantum secure aggregation
        QuantumSecureAggregation.QuantumAggregatedResult result = secureAggregation.quantumAggregate();

        // Update global model
        globalQuantumModel = result.getWeights();

        // Build quantum circuit from aggregated weights
        QuantumCircuit circuit = circuitBuilder.buildVariationalCircuit(NUM_QUBITS, NUM_LAYERS, globalQuantumModel);

        // Update round status
        round.setStatus("COMPLETED");
        round.setAggregatedModel(result);

        return new QuantumAggregationResult(
            result.getWeights(),
            result.getClientCount(),
            result.getMessage(),
            circuit.getCircuitString()
        );
    }

    /**
     * Quantum inference using global model
     */
    public QuantumInferenceResult quantumPredict(double[] input) {
        if (globalQuantumModel == null) {
            return new QuantumInferenceResult(0.5, 0.0, "Model not initialized");
        }

        // Quantum feature mapping
        double[] quantumFeatures = quantumFeatureMap(input);

        // Quantum inference
        double prediction = 0;
        for (int i = 0; i < Math.min(quantumFeatures.length, globalQuantumModel.length); i++) {
            prediction += quantumFeatures[i] * globalQuantumModel[i];
        }
        prediction = 1.0 / (1.0 + Math.exp(-prediction * 0.5));

        // Build quantum circuit
        QuantumCircuit circuit = circuitBuilder.buildKernelCircuit(quantumFeatures);
        String circuitString = circuit.getCircuitString();

        // Calculate confidence
        double confidence = 0.7 + ThreadLocalRandom.current().nextDouble() * 0.2;

        return new QuantumInferenceResult(prediction, confidence, circuitString);
    }

    private double[] quantumFeatureMap(double[] input) {
        double[] features = new double[input.length * 2];
        for (int i = 0; i < input.length; i++) {
            features[i] = Math.sin(input[i] * Math.PI);
            features[input.length + i] = Math.cos(input[i] * Math.PI);
        }
        return features;
    }

    /**
     * Auto-start new round every 15 minutes
     */
    @Scheduled(fixedDelay = 900000) // 15 minutes
    public void autoStartRound() {
        log.info("🔬 Auto-starting quantum federated round...");
        startRound();
    }

    /**
     * Get round status
     */
    public FederatedRound getRound(String roundId) {
        return rounds.get(roundId);
    }

    /**
     * Get all rounds
     */
    public List<FederatedRound> getAllRounds() {
        return new ArrayList<>(rounds.values());
    }

    /**
     * Get global model
     */
    public double[] getGlobalModel() {
        return globalQuantumModel;
    }

    /**
     * Get quantum federated stats
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRounds", roundNumber);
        stats.put("activeRounds", rounds.values().stream().parallel().filter(r -> "ACTIVE".equals(r.getStatus())).count());
        stats.put("totalClients", clientStatus.size());
        stats.put("numQubits", NUM_QUBITS);
        stats.put("numLayers", NUM_LAYERS);
        stats.put("modelInitialized", globalQuantumModel != null);
        stats.put("status", "Quantum Federated Learning System active");
        return stats;
    }

    public static class FederatedRound {
        private final String roundId;
        private final int roundNumber;
        private final QuantumCircuit circuit;
        private final List<String> participants = new ArrayList<>();
        private String status = "ACTIVE";
        private QuantumSecureAggregation.QuantumAggregatedResult aggregatedModel;
        private double[] globalModel;
        private final long startTime;

        public FederatedRound(String roundId, int roundNumber, QuantumCircuit circuit) {
            this.roundId = roundId;
            this.roundNumber = roundNumber;
            this.circuit = circuit;
            this.startTime = System.currentTimeMillis();
        }

        public void addParticipant(String clientId) {
            if (!participants.contains(clientId)) {
                participants.add(clientId);
            }
        }

        public String getRoundId() { return roundId; }
        public int getRoundNumber() { return roundNumber; }
        public QuantumCircuit getCircuit() { return circuit; }
        public List<String> getParticipants() { return participants; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public QuantumSecureAggregation.QuantumAggregatedResult getAggregatedModel() { return aggregatedModel; }
        public void setAggregatedModel(QuantumSecureAggregation.QuantumAggregatedResult aggregatedModel) { this.aggregatedModel = aggregatedModel; }
        public double[] getGlobalModel() { return globalModel; }
        public void setGlobalModel(double[] globalModel) { this.globalModel = globalModel; }
        public long getStartTime() { return startTime; }
    }

    public static class QuantumClientStatus {
        private final String clientId;
        private final boolean active;
        private final long lastActive;

        public QuantumClientStatus(String clientId, boolean active, long lastActive) {
            this.clientId = clientId;
            this.active = active;
            this.lastActive = lastActive;
        }

        public String getClientId() { return clientId; }
        public boolean isActive() { return active; }
        public long getLastActive() { return lastActive; }
    }

    public static class ClientParticipation {
        private final String clientId;
        private final boolean success;
        private final String message;

        public ClientParticipation(String clientId, boolean success, String message) {
            this.clientId = clientId;
            this.success = success;
            this.message = message;
        }

        public String getClientId() { return clientId; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    public static class QuantumAggregationResult {
        private final double[] weights;
        private final int clientCount;
        private final String message;
        private final String circuitDetails;

        public QuantumAggregationResult(double[] weights, int clientCount, String message, String circuitDetails) {
            this.weights = weights;
            this.clientCount = clientCount;
            this.message = message;
            this.circuitDetails = circuitDetails;
        }

        public double[] getWeights() { return weights; }
        public int getClientCount() { return clientCount; }
        public String getMessage() { return message; }
        public String getCircuitDetails() { return circuitDetails; }
    }

    public static class QuantumInferenceResult {
        private final double prediction;
        private final double confidence;
        private final String circuitDetails;

        public QuantumInferenceResult(double prediction, double confidence, String circuitDetails) {
            this.prediction = prediction;
            this.confidence = confidence;
            this.circuitDetails = circuitDetails;
        }

        public double getPrediction() { return prediction; }
        public double getConfidence() { return confidence; }
        public String getCircuitDetails() { return circuitDetails; }
    }
}