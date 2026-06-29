package com.vcsm.healing;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnomalyDetector {

    private final Map<String, List<Double>> metricHistory = new ConcurrentHashMap<>();
    private final Map<String, Double> baselineMetrics = new ConcurrentHashMap<>();

    /**
     * Detect anomalies in system metrics
     */
    public AnomalyResult detectAnomalies(Map<String, Double> currentMetrics) {
        List<Anomaly> anomalies = new ArrayList<>();

        for (Map.Entry<String, Double> entry : currentMetrics.entrySet()) {
            String metricName = entry.getKey();
            double value = entry.getValue();

            // Update history
            metricHistory.computeIfAbsent(metricName, k -> new ArrayList<>()).add(value);

            // Calculate baseline
            double baseline = calculateBaseline(metricName);
            baselineMetrics.put(metricName, baseline);

            // Detect anomaly
            if (value > baseline * 1.5) {
                anomalies.add(new Anomaly(
                    metricName,
                    value,
                    baseline,
                    "HIGH",
                    "Value exceeds baseline by " + String.format("%.0f%%", ((value - baseline) / baseline) * 100)
                ));
            } else if (value < baseline * 0.5) {
                anomalies.add(new Anomaly(
                    metricName,
                    value,
                    baseline,
                    "LOW",
                    "Value below baseline by " + String.format("%.0f%%", ((baseline - value) / baseline) * 100)
                ));
            }
        }

        return new AnomalyResult(anomalies, !anomalies.isEmpty());
    }

    private double calculateBaseline(String metricName) {
        List<Double> history = metricHistory.get(metricName);
        if (history == null || history.size() < 3) {
            return 1.0;
        }

        // Use median of last 10 values as baseline
        int start = Math.max(0, history.size() - 10);
        List<Double> recent = history.subList(start, history.size());
        Collections.sort(recent);
        return recent.get(recent.size() / 2);
    }

    public static class Anomaly {
        private final String metricName;
        private final double currentValue;
        private final double baseline;
        private final String severity;
        private final String description;

        public Anomaly(String metricName, double currentValue, double baseline, String severity, String description) {
            this.metricName = metricName;
            this.currentValue = currentValue;
            this.baseline = baseline;
            this.severity = severity;
            this.description = description;
        }

        public String getMetricName() { return metricName; }
        public double getCurrentValue() { return currentValue; }
        public double getBaseline() { return baseline; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
    }

    public static class AnomalyResult {
        private final List<Anomaly> anomalies;
        private final boolean hasAnomalies;

        public AnomalyResult(List<Anomaly> anomalies, boolean hasAnomalies) {
            this.anomalies = anomalies;
            this.hasAnomalies = hasAnomalies;
        }

        public List<Anomaly> getAnomalies() { return anomalies; }
        public boolean isHasAnomalies() { return hasAnomalies; }
    }
}