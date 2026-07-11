package com.vcsm.transfer;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KnowledgeTransfer {

    private final Map<String, KnowledgeBase> knowledgeBases = new ConcurrentHashMap<>();
    private final Map<String, List<KnowledgeTransferRecord>> transferHistory = new ConcurrentHashMap<>();

    /**
     * Create a knowledge base for a domain
     */
    public KnowledgeBase createKnowledgeBase(String domain, double[] data, double[] labels) {
        KnowledgeBase kb = new KnowledgeBase(domain, data, labels);
        knowledgeBases.put(domain, kb);
        return kb;
    }

    /**
     * Transfer knowledge from source to target domain
     */
    public KnowledgeTransferResult transferKnowledge(String sourceDomain, String targetDomain, int numSamples) {
        KnowledgeBase source = knowledgeBases.get(sourceDomain);
        KnowledgeBase target = knowledgeBases.get(targetDomain);

        if (source == null || target == null) {
            throw new CustomDomainException("Knowledge base not found");
        }

        // Extract knowledge from source
        double[][] sourceData = source.getData();
        double[] sourceLabels = source.getLabels();

        // Select knowledge to transfer (top N samples)
        int transferCount = Math.min(numSamples, sourceData.length);
        double[][] transferredData = new double[transferCount][];
        double[] transferredLabels = new double[transferCount];

        for (int i = 0; i < transferCount; i++) {
            transferredData[i] = sourceData[i];
            transferredLabels[i] = sourceLabels[i];
        }

        // Add to target knowledge base
        target.addKnowledge(transferredData, transferredLabels);

        // Record transfer
        KnowledgeTransferRecord record = new KnowledgeTransferRecord(
            sourceDomain,
            targetDomain,
            transferCount,
            System.currentTimeMillis()
        );
        transferHistory.computeIfAbsent(targetDomain, k -> new ArrayList<>()).add(record);

        return new KnowledgeTransferResult(
            sourceDomain,
            targetDomain,
            transferCount,
            transferredData,
            transferredLabels,
            source.getData().length - transferCount,
            target.getData().length
        );
    }

    /**
     * Get transfer history for a domain
     */
    public List<KnowledgeTransferRecord> getTransferHistory(String domain) {
        return transferHistory.getOrDefault(domain, new ArrayList<>());
    }

    /**
     * Get knowledge base
     */
    public KnowledgeBase getKnowledgeBase(String domain) {
        return knowledgeBases.get(domain);
    }

    /**
     * Get all knowledge bases
     */
    public Map<String, KnowledgeBase> getAllKnowledgeBases() {
        return new HashMap<>(knowledgeBases);
    }

    public static class KnowledgeBase {
        private final String domain;
        private final List<double[]> data = new ArrayList<>();
        private final List<Double> labels = new ArrayList<>();
        private long lastUpdated;

        public KnowledgeBase(String domain, double[][] initialData, double[] initialLabels) {
            this.domain = domain;
            for (double[] d : initialData) {
                data.add(d);
            }
            for (double l : initialLabels) {
                labels.add(l);
            }
            this.lastUpdated = System.currentTimeMillis();
        }

        public void addKnowledge(double[][] newData, double[] newLabels) {
            for (double[] d : newData) {
                data.add(d);
            }
            for (double l : newLabels) {
                labels.add(l);
            }
            lastUpdated = System.currentTimeMillis();
        }

        public String getDomain() { return domain; }
        public double[][] getData() { return data.toArray(new double[0][]); }
        public double[] getLabels() { return labels.stream().mapToDouble(Double::doubleValue).toArray(); }
        public long getLastUpdated() { return lastUpdated; }
        public int getSize() { return data.size(); }
    }

    public static class KnowledgeTransferRecord {
        private final String sourceDomain;
        private final String targetDomain;
        private final int transferCount;
        private final long timestamp;

        public KnowledgeTransferRecord(String sourceDomain, String targetDomain, int transferCount, long timestamp) {
            this.sourceDomain = sourceDomain;
            this.targetDomain = targetDomain;
            this.transferCount = transferCount;
            this.timestamp = timestamp;
        }

        public String getSourceDomain() { return sourceDomain; }
        public String getTargetDomain() { return targetDomain; }
        public int getTransferCount() { return transferCount; }
        public long getTimestamp() { return timestamp; }
    }

    public static class KnowledgeTransferResult {
        private final String sourceDomain;
        private final String targetDomain;
        private final int transferCount;
        private final double[][] transferredData;
        private final double[] transferredLabels;
        private final int remainingSource;
        private final int targetSize;

        public KnowledgeTransferResult(String sourceDomain, String targetDomain, int transferCount,
                                       double[][] transferredData, double[] transferredLabels,
                                       int remainingSource, int targetSize) {
            this.sourceDomain = sourceDomain;
            this.targetDomain = targetDomain;
            this.transferCount = transferCount;
            this.transferredData = transferredData;
            this.transferredLabels = transferredLabels;
            this.remainingSource = remainingSource;
            this.targetSize = targetSize;
        }

        public String getSourceDomain() { return sourceDomain; }
        public String getTargetDomain() { return targetDomain; }
        public int getTransferCount() { return transferCount; }
        public double[][] getTransferredData() { return transferredData; }
        public double[] getTransferredLabels() { return transferredLabels; }
        public int getRemainingSource() { return remainingSource; }
        public int getTargetSize() { return targetSize; }
    }
}