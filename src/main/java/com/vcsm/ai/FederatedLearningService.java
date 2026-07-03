package com.vcsm.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@lombok.RequiredArgsConstructor
public class FederatedLearningService {

    private final LocalModelTrainer localModelTrainer;

    private final SecureAggregator secureAggregator;

    private final PrivacyEngine privacyEngine;

    private final Map<String, FederatedRound> rounds = new ConcurrentHashMap<>();
    private int roundNumber = 0;

    /**
     * Start a new federated learning round
     */
    public FederatedRound startRound() {
        roundNumber++;
        String roundId = "round_" + roundNumber + "_" + System.currentTimeMillis();
        FederatedRound round = new FederatedRound(roundId, roundNumber);
        rounds.put(roundId, round);
        return round;
    }

    /**
     * Client participates in federated learning
     */
    public ClientParticipation participate(String roundId, String clientId, List<double[]> features, List<Double> labels) {
        FederatedRound round = rounds.get(roundId);
        if (round == null) {
            return new ClientParticipation(clientId, false, "Round not found");
        }

        // Train local model
        LocalModelTrainer.LocalModelResult result = localModelTrainer.trainLocalModel(features, labels, 5);

        // Send update to aggregator
        secureAggregator.addLocalUpdate(clientId, result.getWeights(), result.getDataSize());

        round.addParticipant(clientId);

        return new ClientParticipation(
            clientId,
            true,
            "Local training complete. Accuracy: " + String.format("%.2f", result.getAccuracy())
        );
    }

    /**
     * Aggregate models and update global model
     */
    public AggregatedGlobalModel aggregate(String roundId) {
        FederatedRound round = rounds.get(roundId);
        if (round == null) {
            return new AggregatedGlobalModel(null, 0, "Round not found");
        }

        SecureAggregator.AggregatedModel aggregated = secureAggregator.aggregate(roundId);

        // Update round status
        round.setStatus("COMPLETED");
        round.setAggregatedModel(aggregated);

        // Generate privacy report
        PrivacyEngine.PrivacyReport privacyReport = privacyEngine.getPrivacyReport(roundNumber);

        return new AggregatedGlobalModel(
            aggregated.getWeights(),
            aggregated.getClientCount(),
            "Aggregation complete. " + aggregated.getMessage() + " | " + privacyReport.getRecommendation()
        );
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
     * Auto-start new round every 10 minutes
     */
    @Scheduled(fixedDelay = 600000) // 10 minutes
    public void autoStartRound() {
        log.info("🧬 Auto-starting federated learning round...");
        startRound();
    }

    public static class FederatedRound {
        private final String roundId;
        private final int roundNumber;
        private final List<String> participants = new ArrayList<>();
        private String status = "ACTIVE";
        private SecureAggregator.AggregatedModel aggregatedModel;
        private final long startTime;

        public FederatedRound(String roundId, int roundNumber) {
            this.roundId = roundId;
            this.roundNumber = roundNumber;
            this.startTime = System.currentTimeMillis();
        }

        public void addParticipant(String clientId) {
            if (!participants.contains(clientId)) {
                participants.add(clientId);
            }
        }

        public String getRoundId() { return roundId; }
        public int getRoundNumber() { return roundNumber; }
        public List<String> getParticipants() { return participants; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public SecureAggregator.AggregatedModel getAggregatedModel() { return aggregatedModel; }
        public void setAggregatedModel(SecureAggregator.AggregatedModel aggregatedModel) { this.aggregatedModel = aggregatedModel; }
        public long getStartTime() { return startTime; }
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

    public static class AggregatedGlobalModel {
        private final double[] weights;
        private final int clientCount;
        private final String message;

        public AggregatedGlobalModel(double[] weights, int clientCount, String message) {
            this.weights = weights;
            this.clientCount = clientCount;
            this.message = message;
        }

        public double[] getWeights() { return weights; }
        public int getClientCount() { return clientCount; }
        public String getMessage() { return message; }
    }
}
