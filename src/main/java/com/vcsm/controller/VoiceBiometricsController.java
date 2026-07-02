
package com.vcsm.controller;

import com.vcsm.security.service.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.vcsm.model.User;
import com.vcsm.model.VoiceVerificationRequest;
import com.vcsm.model.VoiceVerificationResponse;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.VoiceBiometricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/voice/biometrics")
public class VoiceBiometricsController {

    @Autowired
    private VoiceBiometricsService voiceBiometricsService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/enroll")
    public ResponseEntity<VoiceVerificationResponse> enrollVoice(
            @PathVariable Long userId,
            @Valid @RequestBody VoiceVerificationRequest request) {
        
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody VoiceVerificationRequest request) {

        // Security fix:
        // Never trust a client-supplied user ID.
        // Always use the authenticated user's identity from the security context.
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new VoiceVerificationResponse(false, 0, "Authentication required"));
        }

        Long userId = userDetails.getId();

        Optional<User> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new VoiceVerificationResponse(false, 0, "User not found"));
        }

        double duration = 3.0;
        VoiceVerificationResponse response = voiceBiometricsService.enrollVoice(
                userId, request.getVoiceSample(), duration, request.getLanguage(), request.getText());

        if (response.isVerified()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<VoiceVerificationResponse> verifyVoice(
            @Valid @RequestBody VoiceVerificationRequest request) {
        
            @Valid @RequestBody VoiceVerificationRequest request) {

        if (request.getUserId() == null) {
            return ResponseEntity.badRequest()
                    .body(new VoiceVerificationResponse(false, 0, "UserId is required"));
        }

        if (request.getVoiceSample() == null || request.getVoiceSample().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new VoiceVerificationResponse(false, 0, "Voice sample is required"));
        }

        VoiceVerificationResponse response = voiceBiometricsService.verifyVoice(
                request.getUserId(), request.getVoiceSample(), request.getText());

        if (response.isVerified()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getEnrollmentStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Security fix:
        // Prevent users from viewing another user's voice enrollment
        // status by supplying arbitrary user IDs.
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long userId = userDetails.getId();
        boolean isEnrolled = voiceBiometricsService.isVoiceEnrolled(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("isVoiceEnrolled", isEnrolled);

        Optional<User> user = userRepository.findById(userId);
        if (user.isPresent()) {
            response.put("userName", user.get().getName());
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<VoiceVerificationResponse> deleteVoicePrint(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Security fix:
        // Prevent users from deleting another user's voice print
        // by supplying arbitrary user IDs.
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new VoiceVerificationResponse(false, 0, "Authentication required"));
        }

        Long userId = userDetails.getId();

        VoiceVerificationResponse response = voiceBiometricsService.deleteVoicePrint(userId);
        if (response.isVerified()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}