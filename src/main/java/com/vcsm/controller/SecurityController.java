package com.vcsm.controller;

import com.vcsm.security.DeepfakeDetector;
import com.vcsm.security.FraudAlertService;
import com.vcsm.security.VoiceLivenessService;
import com.vcsm.service.VoiceOtpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/security")
@lombok.RequiredArgsConstructor
public class SecurityController {

    private final DeepfakeDetector deepfakeDetector;

    private final VoiceLivenessService livenessService;

    private final FraudAlertService fraudAlertService;

    private final VoiceOtpService voiceOtpService;

    @PostMapping("/detect-deepfake")
    public ResponseEntity<DeepfakeDetector.DeepfakeAnalysis> detectDeepfake(
            @RequestParam String userId,
            @Valid @RequestBody byte[] audioData) {
        
        DeepfakeDetector.DeepfakeAnalysis analysis = deepfakeDetector.analyze(audioData, userId);
        
        if (analysis.isDeepfake()) {
            fraudAlertService.raiseAlert(userId, "Deepfake voice detected", "HIGH");
        }
        
        return ResponseEntity.ok(analysis);
    }

    @PostMapping("/liveness/challenge")
    public ResponseEntity<VoiceLivenessService.LivenessSession> createLivenessChallenge(
            @RequestParam String userId) {
        VoiceLivenessService.LivenessSession session = livenessService.createSession(userId);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/liveness/verify")
    public ResponseEntity<VoiceLivenessService.LivenessResult> verifyLiveness(
            @RequestParam String sessionId,
            @RequestParam String response) {
        VoiceLivenessService.LivenessResult result = livenessService.verifyLiveness(sessionId, response);
        
        if (!result.isVerified()) {
            fraudAlertService.raiseAlert(
                livenessService.getSession(sessionId).getUserId(),
                "Liveness verification failed",
                "MEDIUM"
            );
        }
        
        return ResponseEntity.ok(result);
    }

    @PostMapping("/otp/challenge")
    public ResponseEntity<VoiceOtpService.VoiceOtpSession> createOtpChallenge(
            @RequestParam Long userId) {
        VoiceOtpService.VoiceOtpSession session = voiceOtpService.generateChallenge(userId);
        return ResponseEntity.ok(session);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<MfaVerificationResult> verifyOtp(
            @RequestParam String sessionId,
            @RequestParam String voiceSample) {
        VoiceOtpService.VoiceOtpSessionVerificationResult result = voiceOtpService.verifyOtp(sessionId, voiceSample);
        
        if (!result.isVerified() && result.getUserId() != null) {
            fraudAlertService.raiseAlert(
                String.valueOf(result.getUserId()),
                "Voice MFA OTP verification failed",
                "HIGH"
            );
        }
        
        MfaVerificationResult response = new MfaVerificationResult(
            result.isVerified(),
            result.getMessage(),
            result.getTranscript(),
            result.getBiometricsScore()
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/fraud/alerts")
    public ResponseEntity<?> getAlerts(@RequestParam(required = false) String userId) {
        if (userId != null) {
            return ResponseEntity.ok(fraudAlertService.getUserAlerts(userId));
        }
        return ResponseEntity.ok(fraudAlertService.getAllAlerts());
    }

    @GetMapping("/fraud/score")
    public ResponseEntity<Map<String, Object>> getSuspicionScore(@RequestParam String userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("score", fraudAlertService.getSuspicionScore(userId));
        response.put("status", fraudAlertService.getSuspicionScore(userId) > 50 ? "HIGH_RISK" : "NORMAL");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/fraud/reset")
    public ResponseEntity<Map<String, String>> resetScore(@RequestParam String userId) {
        fraudAlertService.resetSuspicionScore(userId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Suspicion score reset"));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getSecurityStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("deepfakeDetection", "active");
        status.put("livenessDetection", "active");
        status.put("fraudAlertSystem", "active");
        status.put("totalAlerts", fraudAlertService.getAllAlerts().size());
        return ResponseEntity.ok(status);
    }

    public static class MfaVerificationResult {
        private final boolean verified;
        private final String message;
        private final String transcript;
        private final double biometricsScore;

        public MfaVerificationResult(boolean verified, String message, String transcript, double biometricsScore) {
            this.verified = verified;
            this.message = message;
            this.transcript = transcript;
            this.biometricsScore = biometricsScore;
        }

        public boolean isVerified() { return verified; }
        public String getMessage() { return message; }
        public String getTranscript() { return transcript; }
        public double getBiometricsScore() { return biometricsScore; }
    }
}