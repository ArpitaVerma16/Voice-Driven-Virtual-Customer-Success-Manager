package com.vcsm.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ABTestingService {

    private final Map<String, Map<String, TestVariant>> activeTests = new ConcurrentHashMap<>();

    /**
     * Create a new A/B test
     */
    public void createTest(String testName, String... variants) {
        Map<String, TestVariant> variantMap = new HashMap<>();
        for (String variant : variants) {
            variantMap.put(variant, new TestVariant(variant));
        }
        activeTests.put(testName, variantMap);
    }

    /**
     * Get variant for a user
     */
    public String getVariant(String testName, String userId) {
        Map<String, TestVariant> test = activeTests.get(testName);
        if (test == null || test.isEmpty()) {
            return null;
        }

        // Deterministic assignment based on user ID
        int hash = Math.abs(userId.hashCode());
        List<String> variants = new ArrayList<>(test.keySet());
        int index = hash % variants.size();
        return variants.get(index);
    }

    /**
     * Record a conversion for a variant
     */
    public void recordConversion(String testName, String variant, boolean success) {
        Map<String, TestVariant> test = activeTests.get(testName);
        if (test != null) {
            TestVariant v = test.get(variant);
            if (v != null) {
                v.recordConversion(success);
            }
        }
    }

    /**
     * Get test results
     */
    public Map<String, Object> getTestResults(String testName) {
        Map<String, TestVariant> test = activeTests.get(testName);
        if (test == null) {
            return Map.of("error", "Test not found");
        }

        Map<String, Object> results = new HashMap<>();
        results.put("testName", testName);
        results.put("variants", test);
        results.put("totalConversions", test.values().stream()
            .mapToInt(TestVariant::getConversions).sum());
        results.put("bestVariant", getBestVariant(testName));

        return results;
    }

    private String getBestVariant(String testName) {
        Map<String, TestVariant> test = activeTests.get(testName);
        if (test == null || test.isEmpty()) return null;

        return test.entrySet().stream()
            .max((a, b) -> Double.compare(
                a.getValue().getConversionRate(),
                b.getValue().getConversionRate()
            ))
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    public static class TestVariant {
        private final String name;
        private int impressions = 0;
        private int conversions = 0;

        public TestVariant(String name) {
            this.name = name;
        }

        public void recordConversion(boolean success) {
            impressions++;
            if (success) conversions++;
        }

        public double getConversionRate() {
            return impressions > 0 ? (conversions * 100.0 / impressions) : 0;
        }

        public String getName() { return name; }
        public int getImpressions() { return impressions; }
        public int getConversions() { return conversions; }
    }
}