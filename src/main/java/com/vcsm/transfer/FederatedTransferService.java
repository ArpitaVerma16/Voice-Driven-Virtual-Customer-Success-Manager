package com.vcsm.transfer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Service
@lombok.RequiredArgsConstructor
public class FederatedTransferService {
    private static final Logger log = LoggerFactory.getLogger(FederatedTransferService.class);

    private static final Logger log = LoggerFactory.getLogger(FederatedTransferService.class);

    private final DomainAdapter domainAdapter;

    private final KnowledgeTransfer knowledgeTransfer;

    private final Map<String, FederatedNode> nodes = new ConcurrentHashMap<>();
    private final Map<String, List<TransferRound>> transferRounds = new ConcurrentHashMap<>();
    private int roundNumber = 0;

    /**
     * Register a federated node
     */
    public FederatedNode registerNode(String nodeId, String domain, double[] domainFeatures) {
        DomainAdapter.DomainModel domainModel = domainAdapter.registerDomain(domain, domainFeatures);
        FederatedNode node = new FederatedNode(nodeId, domain, domainModel);
        nodes.put(nodeId, node);
        return node;
    }

    /**
     * Start transfer learning round
     */
    public TransferRound startTransferRound(String sourceNodeId, String targetNodeId, int numSamples) {
        FederatedNode source = nodes.get(sourceNodeId);
        FederatedNode target = nodes.get(targetNodeId);

        if (source == null || target == null) {
            throw new CustomDomainException("Node not found");
        }

        roundNumber++;
        String roundId = "FT_" + roundNumber + "_" + System.currentTimeMillis();

        // Check if domains are compatible
        double similarity = domainAdapter.getDomainSimilarity(source.getDomain(), target.getDomain());

        // Perform knowledge transfer
        KnowledgeTransfer.KnowledgeTransferResult result = knowledgeTransfer.transferKnowledge(
            source.getDomain(),
            target.getDomain(),
            numSamples
        );

        TransferRound round = new TransferRound(
            roundId,
            sourceNodeId,
            targetNodeId,
            similarity,
            result.getTransferCount(),
            System.currentTimeMillis()
        );

        transferRounds.computeIfAbsent(targetNodeId, k -> new ArrayList<>()).add(round);

        return round;
    }

    /**
     * Adaptive transfer based on domain similarity
     */
    public AdaptiveTransferResult adaptiveTransfer(String sourceNodeId, String targetNodeId) {
        FederatedNode source = nodes.get(sourceNodeId);
        FederatedNode target = nodes.get(targetNodeId);

        if (source == null || target == null) {
            throw new CustomDomainException("Node not found");
        }

        double similarity = domainAdapter.getDomainSimilarity(source.getDomain(), target.getDomain());

        // Adaptive sample count based on similarity
        int sampleCount = (int) (10 + (1.0 - similarity) * 90);

        // Perform adaptive transfer
        KnowledgeTransfer.KnowledgeTransferResult result = knowledgeTransfer.transferKnowledge(
            source.getDomain(),
            target.getDomain(),
            sampleCount
        );

        return new AdaptiveTransferResult(
            sourceNodeId,
            targetNodeId,
            similarity,
            sampleCount,
            result.getTransferCount(),
            result.getTargetSize()
        );
    }

    /**
     * Get node information
     */
    public FederatedNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    /**
     * Get all nodes
     */
    public List<FederatedNode> getAllNodes() {
        return new ArrayList<>(nodes.values());
    }

    /**
     * Get transfer rounds for a node
     */
    public List<TransferRound> getTransferRounds(String nodeId) {
        return transferRounds.getOrDefault(nodeId, new ArrayList<>());
    }

    /**
     * Auto-trigger transfer learning
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    public void autoTransfer() {
        log.info("🔄 Auto-triggering federated transfer learning...");
        List<FederatedNode> nodeList = getAllNodes();
        if (nodeList.size() >= 2) {
            // Transfer from first node to second
            FederatedNode source = nodeList.get(0);
            FederatedNode target = nodeList.get(1);
            adaptiveTransfer(source.getNodeId(), target.getNodeId());
        }
    }

    /**
     * Get stats
     */
    public Map<String, Object> getTransferStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNodes", nodes.size());
        stats.put("totalRounds", transferRounds.values().stream().mapToInt(List::size).sum());
        stats.put("roundNumber", roundNumber);
        stats.put("status", "Federated Transfer Learning active");
        return stats;
    }

    public static class FederatedNode {
        private final String nodeId;
        private final String domain;
        private final DomainAdapter.DomainModel domainModel;
        private boolean active = true;
        private long lastActive;

        public FederatedNode(String nodeId, String domain, DomainAdapter.DomainModel domainModel) {
            this.nodeId = nodeId;
            this.domain = domain;
            this.domainModel = domainModel;
            this.lastActive = System.currentTimeMillis();
        }

        public String getNodeId() { return nodeId; }
        public String getDomain() { return domain; }
        public DomainAdapter.DomainModel getDomainModel() { return domainModel; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public long getLastActive() { return lastActive; }
        public void setLastActive(long lastActive) { this.lastActive = lastActive; }
    }

    public static class TransferRound {
        private final String roundId;
        private final String sourceNodeId;
        private final String targetNodeId;
        private final double domainSimilarity;
        private final int transferredSamples;
        private final long timestamp;

        public TransferRound(String roundId, String sourceNodeId, String targetNodeId,
                            double domainSimilarity, int transferredSamples, long timestamp) {
            this.roundId = roundId;
            this.sourceNodeId = sourceNodeId;
            this.targetNodeId = targetNodeId;
            this.domainSimilarity = domainSimilarity;
            this.transferredSamples = transferredSamples;
            this.timestamp = timestamp;
        }

        public String getRoundId() { return roundId; }
        public String getSourceNodeId() { return sourceNodeId; }
        public String getTargetNodeId() { return targetNodeId; }
        public double getDomainSimilarity() { return domainSimilarity; }
        public int getTransferredSamples() { return transferredSamples; }
        public long getTimestamp() { return timestamp; }
    }

    public static class AdaptiveTransferResult {
        private final String sourceNodeId;
        private final String targetNodeId;
        private final double similarity;
        private final int requestedSamples;
        private final int transferredSamples;
        private final int targetSize;

        public AdaptiveTransferResult(String sourceNodeId, String targetNodeId, double similarity,
                                     int requestedSamples, int transferredSamples, int targetSize) {
            this.sourceNodeId = sourceNodeId;
            this.targetNodeId = targetNodeId;
            this.similarity = similarity;
            this.requestedSamples = requestedSamples;
            this.transferredSamples = transferredSamples;
            this.targetSize = targetSize;
        }

        public String getSourceNodeId() { return sourceNodeId; }
        public String getTargetNodeId() { return targetNodeId; }
        public double getSimilarity() { return similarity; }
        public int getRequestedSamples() { return requestedSamples; }
        public int getTransferredSamples() { return transferredSamples; }
        public int getTargetSize() { return targetSize; }
    }
}