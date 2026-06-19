
package com.vcsm.controller;

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
@CrossOrigin(origins = "*")
public class VoiceBiometricsController {
    
    @Autowired
    private VoiceBiometricsService voiceBiometricsService;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/enroll/{userId}")
    public ResponseEntity<VoiceVerificationResponse> enrollVoice(
            @PathVariable Long userId,
            @RequestBody VoiceVerificationRequest request) {
        
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
            @RequestBody VoiceVerificationRequest request) {
        
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
    
    @GetMapping("/status/{userId}")
    public ResponseEntity<Map<String, Object>> getEnrollmentStatus(@PathVariable Long userId) {
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
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<VoiceVerificationResponse> deleteVoicePrint(@PathVariable Long userId) {
        VoiceVerificationResponse response = voiceBiometricsService.deleteVoicePrint(userId);
        
        if (response.isVerified()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }
}