package com.vcsm.service;

import com.vcsm.model.VoiceVerificationResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@lombok.RequiredArgsConstructor
public class VoiceOtpService {

    private final Map<String, VoiceOtpSession> sessions = new ConcurrentHashMap<>();
    private final Random random = new Random();

    private final SpeechToTextService speechToTextService;

    private final VoiceBiometricsService voiceBiometricsService;

    /**
     * Generate a new 4-digit OTP challenge session
     */
    public VoiceOtpSession generateChallenge(Long userId) {
        String sessionId = UUID.randomUUID().toString();
        // Generate random 4-digit code between "1000" and "9999"
        String otpCode = String.format("%04d", random.nextInt(9000) + 1000);
        
        VoiceOtpSession session = new VoiceOtpSession(sessionId, userId, otpCode);
        sessions.put(sessionId, session);
        return session;
    }

    /**
     * Verify the spoken OTP and the speaker's voiceprint
     */
    public VoiceOtpSessionVerificationResult verifyOtp(String sessionId, String base64Audio) {
        VoiceOtpSession session = sessions.get(sessionId);
        if (session == null) {
            return new VoiceOtpSessionVerificationResult(false, "Session not found", "", 0.0, null);
        }

        if (session.isExpired()) {
            sessions.remove(sessionId);
            session.setStatus("EXPIRED");
            return new VoiceOtpSessionVerificationResult(false, "Verification session expired", "", 0.0, session.getUserId());
        }

        // 1. Transcribe the audio
        String transcript = speechToTextService.transcribe(base64Audio, "en-US");
        String normalizedTranscript = normalizeToDigits(transcript);
        String expectedDigits = normalizeToDigits(session.getOtpCode());

        // 2. Check if the correct digits were spoken
        if (normalizedTranscript.isEmpty() || !normalizedTranscript.equals(expectedDigits)) {
            session.setStatus("FAILED");
            sessions.remove(sessionId);
            return new VoiceOtpSessionVerificationResult(
                false, 
                "Incorrect code spoken. Expected: " + session.getOtpCode() + ", but heard digits: " + normalizedTranscript, 
                transcript, 
                0.0, 
                session.getUserId()
            );
        }

        // 3. Format the OTP digits for voice biometrics verification (separated by spaces for natural detection)
        StringBuilder spacedOtp = new StringBuilder();
        for (int i = 0; i < session.getOtpCode().length(); i++) {
            spacedOtp.append(session.getOtpCode().charAt(i)).append(" ");
        }
        String otpText = spacedOtp.toString().trim();

        // 4. Verify speaker voiceprint matching
        VoiceVerificationResponse bioResponse = voiceBiometricsService.verifyVoice(
            session.getUserId(), 
            base64Audio, 
            otpText
        );

        boolean verified = bioResponse.isVerified();
        session.setStatus(verified ? "VERIFIED" : "FAILED");
        sessions.remove(sessionId); // Invalidate session after use

        String message = verified ? 
            "Voice MFA verification successful" : 
            "Voice biometrics matching failed: " + bioResponse.getMessage();

        return new VoiceOtpSessionVerificationResult(
            verified, 
            message, 
            transcript, 
            bioResponse.getConfidence(), 
            session.getUserId()
        );
    }

    /**
     * Get an active session
     */
    public VoiceOtpSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    /**
     * Helper to normalize words or spaced numbers to a simple string of digits
     */
    public String normalizeToDigits(String text) {
        if (text == null) {
            return org.springframework.http.ResponseEntity.ok("");
        }
        String lower = text.toLowerCase();
        
        // Map of English and Hindi words to digits
        Map<String, String> numMap = new HashMap<>();
        numMap.put("zero", "0");
        numMap.put("one", "1");
        numMap.put("two", "2");
        numMap.put("three", "3");
        numMap.put("four", "4");
        numMap.put("five", "5");
        numMap.put("six", "6");
        numMap.put("seven", "7");
        numMap.put("eight", "8");
        numMap.put("nine", "9");
        
        numMap.put("शून्य", "0");
        numMap.put("एक", "1");
        numMap.put("दो", "2");
        numMap.put("तीन", "3");
        numMap.put("चार", "4");
        numMap.put("पाँच", "5");
        numMap.put("छह", "6");
        numMap.put("सात", "7");
        numMap.put("आठ", "8");
        numMap.put("नौ", "9");

        for (Map.Entry<String, String> entry : numMap.entrySet()) {
            lower = lower.replaceAll(entry.getKey(), entry.getValue());
        }

        // Extract digits only
        return lower.replaceAll("[^0-9]", "");
    }

    /**
     * Model representing a Voice OTP verification challenge session
     */
    public static class VoiceOtpSession {
        private final String sessionId;
        private final Long userId;
        private final String otpCode;
        private final long createdAt;
        private final long expiresAt;
        private String status = "PENDING";

        public VoiceOtpSession(String sessionId, Long userId, String otpCode) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.otpCode = otpCode;
            this.createdAt = System.currentTimeMillis();
            this.expiresAt = this.createdAt + 60000; // 60 seconds expiry
        }

        public String getSessionId() { return sessionId; }
        public Long getUserId() { return userId; }
        public String getOtpCode() { return otpCode; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public boolean isExpired() { return System.currentTimeMillis() > expiresAt; }
    }

    /**
     * Result of verifying an OTP session
     */
    public static class VoiceOtpSessionVerificationResult {
        private final boolean verified;
        private final String message;
        private final String transcript;
        private final double biometricsScore;
        private final Long userId;

        public VoiceOtpSessionVerificationResult(boolean verified, String message, String transcript, double biometricsScore, Long userId) {
            this.verified = verified;
            this.message = message;
            this.transcript = transcript;
            this.biometricsScore = biometricsScore;
            this.userId = userId;
        }

        public boolean isVerified() { return verified; }
        public String getMessage() { return message; }
        public String getTranscript() { return transcript; }
        public double getBiometricsScore() { return biometricsScore; }
        public Long getUserId() { return userId; }
    }
}
