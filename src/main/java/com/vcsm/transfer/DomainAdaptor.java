package com.vcsm.transfer;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class DomainAdapter {

    private final Map<String, DomainModel> domainModels = new ConcurrentHashMap<>();
    private final Map<String, DomainMapping> domainMappings = new ConcurrentHashMap<>();

    /**
     * Register a new domain
     */
    public DomainModel registerDomain(String domainName, double[] features) {
        DomainModel model = new DomainModel(domainName, features);
        domainModels.put(domainName, model);
        return model;
    }

    /**
     * Adapt source domain to target domain
     */
    public double[] adaptDomain(String sourceDomain, String targetDomain, double[] sourceFeatures) {
        DomainModel source = domainModels.get(sourceDomain);
        DomainModel target = domainModels.get(targetDomain);

        if (source == null || target == null) {
            throw new RuntimeException("Domain not found");
        }

        // Learn domain mapping
        DomainMapping mapping = learnDomainMapping(source, target);
        domainMappings.put(sourceDomain + "->" + targetDomain, mapping);

        // Transform features
        double[] adaptedFeatures = sourceFeatures.clone();
        for (int i = 0; i < adaptedFeatures.length && i < mapping.getWeights().length; i++) {
            adaptedFeatures[i] *= mapping.getWeights()[i];
            adaptedFeatures[i] += mapping.getBias()[i];
        }

        return adaptedFeatures;
    }

    private DomainMapping learnDomainMapping(DomainModel source, DomainModel target) {
        // Simulate domain adaptation learning
        int featureSize = Math.min(source.getFeatures().length, target.getFeatures().length);
        double[] weights = new double[featureSize];
        double[] bias = new double[featureSize];

        for (int i = 0; i < featureSize; i++) {
            weights[i] = 0.8 + ThreadLocalRandom.current().nextDouble() * 0.4;
            bias[i] = ThreadLocalRandom.current().nextDouble() * 0.1 - 0.05;
        }

        return new DomainMapping(weights, bias);
    }

    /**
     * Get domain similarity
     */
    public double getDomainSimilarity(String domain1, String domain2) {
        DomainModel model1 = domainModels.get(domain1);
        DomainModel model2 = domainModels.get(domain2);

        if (model1 == null || model2 == null) return 0.0;

        double[] features1 = model1.getFeatures();
        double[] features2 = model2.getFeatures();

        double similarity = 0;
        for (int i = 0; i < Math.min(features1.length, features2.length); i++) {
            similarity += Math.abs(features1[i] - features2[i]);
        }

        return 1.0 - (similarity / Math.min(features1.length, features2.length));
    }

    /**
     * Check if domain exists
     */
    public boolean domainExists(String domainName) {
        return domainModels.containsKey(domainName);
    }

    public static class DomainModel {
        private final String name;
        private final double[] features;
        private long lastUpdated;

        public DomainModel(String name, double[] features) {
            this.name = name;
            this.features = features;
            this.lastUpdated = System.currentTimeMillis();
        }

        public String getName() { return name; }
        public double[] getFeatures() { return features; }
        public long getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    }

    public static class DomainMapping {
        private final double[] weights;
        private final double[] bias;

        public DomainMapping(double[] weights, double[] bias) {
            this.weights = weights;
            this.bias = bias;
        }

        public double[] getWeights() { return weights; }
        public double[] getBias() { return bias; }
    }
}