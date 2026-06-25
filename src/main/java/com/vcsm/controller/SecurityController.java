package com.vcsm.controller;

import com.vcsm.security.DeepfakeDetector;
import com.vcsm.security.FraudAlertService;
import com.vcsm.security.VoiceLivenessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/security")
@CrossOrigin(origins = "*")
public class SecurityController {

    @Autowired
    private DeepfakeDetector deepfakeDetector;

    @Autowired
    private VoiceLivenessService livenessService;

    @Autowired
    private FraudAlertService fraudAlertService;

    @PostMapping("/detect-deepfake")
    public ResponseEntity<DeepfakeDetector.DeepfakeAnalysis> detectDeepfake(
            @RequestParam String userId,
            @RequestBody byte[] audioData) {
        
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
}