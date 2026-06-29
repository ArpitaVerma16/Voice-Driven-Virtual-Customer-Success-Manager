package com.vcsm.bci;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BCIService {

    private final Map<String, BCISession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, List<EEGSignal>> signalHistory = new ConcurrentHashMap<>();

    /**
     * Start a BCI session
     */
    public BCISession startSession(String userId) {
        BCISession session = new BCISession(userId, UUID.randomUUID().toString());
        activeSessions.put(session.getSessionId(), session);
        signalHistory.put(userId, new ArrayList<>());
        return session;
    }

    /**
     * Process EEG signals
     */
    public BrainSignalResult processSignal(String sessionId, double[] signalData) {
        BCISession session = activeSessions.get(sessionId);
        if (session == null) {
            return new BrainSignalResult(false, "Session not found", "NONE", 0.0);
        }

        // Process signal
        EEGSignal signal = new EEGSignal(signalData, System.currentTimeMillis());
        signalHistory.get(session.getUserId()).add(signal);

        // Analyze brain activity
        String thought = analyzeThought(signalData);
        double confidence = calculateConfidence(signalData);
        String mentalState = detectMentalState(signalData);

        // Update session
        session.addSignal(signal);
        session.setLastActivity(System.currentTimeMillis());

        return new BrainSignalResult(true, thought, mentalState, confidence);
    }

    private String analyzeThought(double[] signal) {
        // Simulated thought classification
        double avg = calculateAverage(signal);
        double variance = calculateVariance(signal);

        if (avg > 0.5 && variance < 0.1) {
            return "FOCUS";
        } else if (avg > 0.3 && variance > 0.2) {
            return "COMPLAINT";
        } else if (avg < 0.2) {
            return "RELAX";
        } else if (avg > 0.7) {
            return "URGENT";
        }
        return "NEUTRAL";
    }

    private double calculateConfidence(double[] signal) {
        double variance = calculateVariance(signal);
        return Math.min(0.95, 0.7 + (1.0 - variance) * 0.3);
    }

    private String detectMentalState(double[] signal) {
        double avg = calculateAverage(signal);
        if (avg > 0.6) return "HIGH_ACTIVITY";
        if (avg > 0.4) return "MODERATE";
        if (avg > 0.2) return "LOW_ACTIVITY";
        return "RESTING";
    }

    private double calculateAverage(double[] signal) {
        double sum = 0;
        for (double d : signal) sum += d;
        return signal.length > 0 ? sum / signal.length : 0;
    }

    private double calculateVariance(double[] signal) {
        double avg = calculateAverage(signal);
        double sum = 0;
        for (double d : signal) sum += Math.pow(d - avg, 2);
        return signal.length > 0 ? sum / signal.length : 0;
    }

    /**
     * Get session status
     */
    public BCISession getSession(String sessionId) {
        return activeSessions.get(sessionId);
    }

    /**
     * Get signal history for user
     */
    public List<EEGSignal> getSignalHistory(String userId) {
        return signalHistory.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * End BCI session
     */
    public void endSession(String sessionId) {
        BCISession session = activeSessions.remove(sessionId);
        if (session != null) {
            session.setActive(false);
        }
    }

    /**
     * Get BCI stats
     */
    public Map<String, Object> getBCIStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeSessions", activeSessions.size());
        stats.put("totalSignals", signalHistory.values().stream()
            .mapToInt(List::size).sum());
        stats.put("status", "BCI System active");
        return stats;
    }

    public static class BCISession {
        private final String userId;
        private final String sessionId;
        private final List<EEGSignal> signals = new ArrayList<>();
        private long startTime;
        private long lastActivity;
        private boolean active = true;

        public BCISession(String userId, String sessionId) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.startTime = System.currentTimeMillis();
            this.lastActivity = System.currentTimeMillis();
        }

        public void addSignal(EEGSignal signal) {
            signals.add(signal);
        }

        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public List<EEGSignal> getSignals() { return signals; }
        public long getStartTime() { return startTime; }
        public long getLastActivity() { return lastActivity; }
        public void setLastActivity(long lastActivity) { this.lastActivity = lastActivity; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class EEGSignal {
        private final double[] data;
        private final long timestamp;

        public EEGSignal(double[] data, long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }

        public double[] getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }

    public static class BrainSignalResult {
        private final boolean success;
        private final String thought;
        private final String mentalState;
        private final double confidence;

        public BrainSignalResult(boolean success, String thought, String mentalState, double confidence) {
            this.success = success;
            this.thought = thought;
            this.mentalState = mentalState;
            this.confidence = confidence;
        }

        public boolean isSuccess() { return success; }
        public String getThought() { return thought; }
        public String getMentalState() { return mentalState; }
        public double getConfidence() { return confidence; }
    }
}