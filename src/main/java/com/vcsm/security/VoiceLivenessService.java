package com.vcsm.security;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class VoiceLivenessService {

    private final Map<String, LivenessSession> sessions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private final String[] CHALLENGES = {
        "Please say the number: 1234",
        "Please say the word: 'hello'",
        "Please say your name",
        "Please count from 1 to 5",
        "Please say today's date"
    };

    /**
     * Create a liveness challenge session
     */
    public LivenessSession createSession(String userId) {
        String sessionId = UUID.randomUUID().toString();
        String challenge = CHALLENGES[random.nextInt(CHALLENGES.length)];
        
        LivenessSession session = new LivenessSession(sessionId, userId, challenge);
        sessions.put(sessionId, session);
        
        return session;
    }

    /**
     * Verify liveness response
     */
    public LivenessResult verifyLiveness(String sessionId, String responseText) {
        LivenessSession session = sessions.get(sessionId);
        if (session == null) {
            return new LivenessResult(false, "Session not found", 0.0);
        }

        // Simulated verification
        // In production, use speech-to-text and compare
        double score = calculateSimilarity(session.getChallenge(), responseText);
        boolean verified = score > 0.7;

        session.setStatus(verified ? "VERIFIED" : "FAILED");
        session.setResponse(responseText);

        return new LivenessResult(verified, 
            verified ? "Liveness verified successfully" : "Liveness verification failed",
            score);
    }

    private double calculateSimilarity(String challenge, String response) {
        // Simulated similarity calculation
        // In production, use actual text comparison
        String[] challengeWords = challenge.toLowerCase().split(" ");
        String[] responseWords = response.toLowerCase().split(" ");
        
        long matches = 0;
        for (String cw : challengeWords) {
            for (String rw : responseWords) {
                if (rw.contains(cw) && cw.length() > 2) {
                    matches++;
                    break;
                }
            }
        }
        
        return (double) matches / challengeWords.length;
    }

    /**
     * Get session status
     */
    public LivenessSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public static class LivenessSession {
        private final String sessionId;
        private final String userId;
        private final String challenge;
        private String response;
        private String status = "PENDING";
        private final long createdAt;
        private final long expiresAt;

        public LivenessSession(String sessionId, String userId, String challenge) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.challenge = challenge;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = this.createdAt + 30000; // 30 second expiry
        }

        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getChallenge() { return challenge; }
        public String getResponse() { return response; }
        public void setResponse(String response) { this.response = response; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    public static class LivenessResult {
        private final boolean verified;
        private final String message;
        private final double confidence;

        public LivenessResult(boolean verified, String message, double confidence) {
            this.verified = verified;
            this.message = message;
            this.confidence = confidence;
        }

        public boolean isVerified() { return verified; }
        public String getMessage() { return message; }
        public double getConfidence() { return confidence; }
    }
}