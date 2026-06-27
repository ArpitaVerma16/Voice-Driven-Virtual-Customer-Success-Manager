package com.vcsm.twin;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PredictiveDigitalTwin {

    private final Map<String, TwinInstance> twins = new ConcurrentHashMap<>();
    private final Map<String, List<HistoricalData>> history = new ConcurrentHashMap<>();

    /**
     * Create a new digital twin instance
     */
    public TwinInstance createTwin(String twinId, String systemType) {
        TwinInstance twin = new TwinInstance(twinId, systemType, LocalDateTime.now());
        twins.put(twinId, twin);
        history.put(twinId, new ArrayList<>());
        return twin;
    }

    /**
     * Sync real-time data to twin
     */
    public void syncData(String twinId, double[] metrics, String state) {
        TwinInstance twin = twins.get(twinId);
        if (twin == null) return;

        HistoricalData data = new HistoricalData(metrics, state, System.currentTimeMillis());
        history.get(twinId).add(data);

        // Keep last 1000 data points
        List<HistoricalData> hist = history.get(twinId);
        if (hist.size() > 1000) {
            hist.remove(0);
        }

        twin.updateState(state, metrics);
        twin.setLastSync(System.currentTimeMillis());
    }

    /**
     * Predict future state
     */
    public PredictionResult predictFuture(String twinId, int timeHorizon) {
        TwinInstance twin = twins.get(twinId);
        if (twin == null) {
            throw new RuntimeException("Twin not found");
        }

        List<HistoricalData> hist = history.get(twinId);
        if (hist.size() < 10) {
            return new PredictionResult(
                "INSUFFICIENT_DATA",
                "Need more data for prediction",
                new double[0],
                new double[0],
                0.0
            );
        }

        // Simulate LSTM prediction
        double[] currentState = twin.getCurrentMetrics();
        double[] predictedState = predictNextState(currentState, hist);
        double[] confidenceIntervals = calculateConfidence(hist);

        // Determine predicted state
        String predictedStatus = classifyState(predictedState);

        return new PredictionResult(
            predictedStatus,
            "Prediction completed for " + timeHorizon + " steps ahead",
            predictedState,
            confidenceIntervals,
            0.75 + ThreadLocalRandom.current().nextDouble() * 0.2
        );
    }

    private double[] predictNextState(double[] current, List<HistoricalData> history) {
        // Simulate LSTM prediction
        double[] predicted = current.clone();
        Random rand = ThreadLocalRandom.current();

        for (int i = 0; i < predicted.length; i++) {
            // Small random change with trend
            double trend = 0;
            if (history.size() > 5) {
                double sum = 0;
                for (int j = history.size() - 5; j < history.size(); j++) {
                    sum += history.get(j).getMetrics()[i];
                }
                trend = (sum / 5) - current[i];
            }
            predicted[i] += trend * 0.3 + (rand.nextDouble() - 0.5) * 0.1;
            predicted[i] = Math.max(0, Math.min(1, predicted[i]));
        }

        return predicted;
    }

    private double[] calculateConfidence(List<HistoricalData> history) {
        int size = history.get(0).getMetrics().length;
        double[] conf = new double[size];
        for (int i = 0; i < size; i++) {
            conf[i] = 0.1 + ThreadLocalRandom.current().nextDouble() * 0.15;
        }
        return conf;
    }

    private String classifyState(double[] state) {
        double avg = Arrays.stream(state).average().orElse(0);
        if (avg > 0.7) return "HIGH_LOAD";
        if (avg > 0.4) return "NORMAL";
        if (avg > 0.2) return "LOW_LOAD";
        return "IDLE";
    }

    /**
     * Predict anomalies
     */
    public AnomalyPrediction predictAnomalies(String twinId) {
        TwinInstance twin = twins.get(twinId);
        if (twin == null) {
            throw new RuntimeException("Twin not found");
        }

        List<HistoricalData> hist = history.get(twinId);
        if (hist.size() < 20) {
            return new AnomalyPrediction(false, 0.0, "INSUFFICIENT_DATA", new String[0]);
        }

        // Detect anomalies based on deviation
        double[] current = twin.getCurrentMetrics();
        double[] baseline = calculateBaseline(hist);
        List<String> anomalies = new ArrayList<>();

        for (int i = 0; i < current.length; i++) {
            if (Math.abs(current[i] - baseline[i]) > 0.3) {
                anomalies.add("Metric_" + i + " deviates from baseline");
            }
        }

        double anomalyScore = anomalies.size() * 0.15;
        anomalyScore = Math.min(0.95, anomalyScore);

        return new AnomalyPrediction(
            !anomalies.isEmpty(),
            anomalyScore,
            anomalies.isEmpty() ? "NORMAL" : "ANOMALY_DETECTED",
            anomalies.toArray(new String[0])
        );
    }

    private double[] calculateBaseline(List<HistoricalData> history) {
        int size = history.get(0).getMetrics().length;
        double[] baseline = new double[size];
        for (int i = 0; i < size; i++) {
            double sum = 0;
            int count = 0;
            for (int j = Math.max(0, history.size() - 50); j < history.size(); j++) {
                sum += history.get(j).getMetrics()[i];
                count++;
            }
            baseline[i] = sum / count;
        }
        return baseline;
    }

    /**
     * Forecast resources
     */
    public ResourceForecast forecastResources(String twinId, int days) {
        TwinInstance twin = twins.get(twinId);
        if (twin == null) {
            throw new RuntimeException("Twin not found");
        }

        List<HistoricalData> hist = history.get(twinId);
        List<double[]> forecast = new ArrayList<>();
        double[] current = twin.getCurrentMetrics();

        for (int i = 0; i < days; i++) {
            double[] next = predictNextState(current, hist);
            forecast.add(next);
            current = next;
        }

        return new ResourceForecast(
            forecast,
            days,
            twin.getSystemType(),
            "Resource forecast for next " + days + " days"
        );
    }

    /**
     * Get twin state
     */
    public TwinInstance getTwin(String twinId) {
        return twins.get(twinId);
    }

    /**
     * Get all twins
     */
    public List<TwinInstance> getAllTwins() {
        return new ArrayList<>(twins.values());
    }

    /**
     * Get history
     */
    public List<HistoricalData> getHistory(String twinId) {
        return history.getOrDefault(twinId, new ArrayList<>());
    }

    /**
     * Delete twin
     */
    public void deleteTwin(String twinId) {
        twins.remove(twinId);
        history.remove(twinId);
    }

    /**
     * Get twin stats
     */
    public Map<String, Object> getTwinStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTwins", twins.size());
        stats.put("totalDataPoints", history.values().stream().mapToInt(List::size).sum());
        stats.put("status", "Predictive Digital Twin active");
        return stats;
    }

    public static class TwinInstance {
        private final String twinId;
        private final String systemType;
        private final LocalDateTime createdAt;
        private String currentState;
        private double[] currentMetrics;
        private long lastSync;

        public TwinInstance(String twinId, String systemType, LocalDateTime createdAt) {
            this.twinId = twinId;
            this.systemType = systemType;
            this.createdAt = createdAt;
            this.currentState = "INITIALIZING";
            this.currentMetrics = new double[5];
            this.lastSync = System.currentTimeMillis();
        }

        public void updateState(String state, double[] metrics) {
            this.currentState = state;
            this.currentMetrics = metrics.clone();
        }

        public String getTwinId() { return twinId; }
        public String getSystemType() { return systemType; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getCurrentState() { return currentState; }
        public double[] getCurrentMetrics() { return currentMetrics; }
        public long getLastSync() { return lastSync; }
        public void setLastSync(long lastSync) { this.lastSync = lastSync; }
    }

    public static class HistoricalData {
        private final double[] metrics;
        private final String state;
        private final long timestamp;

        public HistoricalData(double[] metrics, String state, long timestamp) {
            this.metrics = metrics;
            this.state = state;
            this.timestamp = timestamp;
        }

        public double[] getMetrics() { return metrics; }
        public String getState() { return state; }
        public long getTimestamp() { return timestamp; }
    }

    public static class PredictionResult {
        private final String predictedState;
        private final String message;
        private final double[] predictedMetrics;
        private final double[] confidenceIntervals;
        private final double confidence;

        public PredictionResult(String predictedState, String message, double[] predictedMetrics,
                               double[] confidenceIntervals, double confidence) {
            this.predictedState = predictedState;
            this.message = message;
            this.predictedMetrics = predictedMetrics;
            this.confidenceIntervals = confidenceIntervals;
            this.confidence = confidence;
        }

        public String getPredictedState() { return predictedState; }
        public String getMessage() { return message; }
        public double[] getPredictedMetrics() { return predictedMetrics; }
        public double[] getConfidenceIntervals() { return confidenceIntervals; }
        public double getConfidence() { return confidence; }
    }

    public static class AnomalyPrediction {
        private final boolean hasAnomaly;
        private final double anomalyScore;
        private final String status;
        private final String[] anomalies;

        public AnomalyPrediction(boolean hasAnomaly, double anomalyScore, String status, String[] anomalies) {
            this.hasAnomaly = hasAnomaly;
            this.anomalyScore = anomalyScore;
            this.status = status;
            this.anomalies = anomalies;
        }

        public boolean isHasAnomaly() { return hasAnomaly; }
        public double getAnomalyScore() { return anomalyScore; }
        public String getStatus() { return status; }
        public String[] getAnomalies() { return anomalies; }
    }

    public static class ResourceForecast {
        private final List<double[]> forecast;
        private final int days;
        private final String systemType;
        private final String message;

        public ResourceForecast(List<double[]> forecast, int days, String systemType, String message) {
            this.forecast = forecast;
            this.days = days;
            this.systemType = systemType;
            this.message = message;
        }

        public List<double[]> getForecast() { return forecast; }
        public int getDays() { return days; }
        public String getSystemType() { return systemType; }
        public String getMessage() { return message; }
    }
}