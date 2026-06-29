package com.vcsm.controller;

import com.vcsm.bci.BCIService;
import com.vcsm.security.service.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bci")
@CrossOrigin(origins = "*")
public class BCIController {

    @Autowired
    private BCIService bciService;

    private boolean isOwnerOrAdmin(CustomUserDetails userDetails, String targetUserId) {
        if (userDetails == null) {
            return false;
        }
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return true;
        }
        return targetUserId != null && (
                targetUserId.equals(String.valueOf(userDetails.getId())) ||
                targetUserId.equals(userDetails.getUsername())
        );
    }

    private boolean canAccessSession(CustomUserDetails userDetails, String sessionId) {
        if (userDetails == null) {
            return false;
        }
        BCIService.BCISession session = bciService.getSession(sessionId);
        if (session == null) {
            return true; // Let the normal flow return 404
        }
        return isOwnerOrAdmin(userDetails, session.getUserId());
    }

    @PostMapping("/start")
    public ResponseEntity<?> startSession(
            @RequestParam(required = false) String userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        
        String targetUserId = (userId != null && !userId.trim().isEmpty()) ? userId : userDetails.getUsername();
        if (!isOwnerOrAdmin(userDetails, targetUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        
        return ResponseEntity.ok(bciService.startSession(targetUserId));
    }

    @PostMapping("/signal")
    public ResponseEntity<?> processSignal(
            @RequestParam String sessionId,
            @RequestBody double[] signalData,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (!canAccessSession(userDetails, sessionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.ok(bciService.processSignal(sessionId, signalData));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (!canAccessSession(userDetails, sessionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        BCIService.BCISession session = bciService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getHistory(
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (!isOwnerOrAdmin(userDetails, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        return ResponseEntity.ok(bciService.getSignalHistory(userId));
    }

    @PostMapping("/end/{sessionId}")
    public ResponseEntity<?> endSession(
            @PathVariable String sessionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Authentication required");
        }
        if (!canAccessSession(userDetails, sessionId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }
        bciService.endSession(sessionId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Session ended"));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(bciService.getBCIStats());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "BCI System active");
        status.put("features", new String[]{
            "EEG Signal Processing",
            "Thought Detection",
            "Mental State Analysis",
            "Real-time Processing",
            "Neurofeedback"
        });
        return ResponseEntity.ok(status);
    }
}