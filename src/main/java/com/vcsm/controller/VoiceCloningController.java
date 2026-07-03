package com.vcsm.controller;

import com.vcsm.model.User;
import com.vcsm.model.VoiceProfile;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.VoiceCloningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voice/cloning")
@lombok.RequiredArgsConstructor
public class VoiceCloningController {

    private final VoiceCloningService voiceCloningService;

    private final UserRepository userRepository;

    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return userRepository.findByEmail(auth.getName()).orElse(null);
    }

    @PostMapping("/clone")
    public ResponseEntity<?> cloneVoice(
            @RequestParam("file") MultipartFile audioFile,
            @RequestParam("name") String profileName) {
        
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        if (audioFile.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Audio file is required"));
        }

        if (audioFile.getSize() > 10 * 1024 * 1024) { // 10MB limit
            return ResponseEntity.badRequest().body(Map.of("error", "File too large. Max 10MB"));
        }

        try {
            VoiceProfile profile = voiceCloningService.cloneVoice(user, audioFile, profileName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Voice cloned successfully");
            response.put("profileId", profile.getId());
            response.put("profileName", profile.getName());
            response.put("isActive", profile.isActive());
            
            return ResponseEntity.ok(response);
            
        } catch (SpecificException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/profiles")
    public ResponseEntity<?> getProfiles() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        List<VoiceProfile> profiles = voiceCloningService.getUserProfiles(user);
        return ResponseEntity.ok(profiles);
    }

    @PostMapping("/profiles/select")
    public ResponseEntity<?> selectProfile(@Valid @RequestBody Map<String, Long> request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Long profileId = request.get("profileId");
        if (profileId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Profile ID required"));
        }

        try {
            VoiceProfile profile = voiceCloningService.selectProfile(user, profileId);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Voice profile selected successfully",
                "profileName", profile.getName()
            ));
        } catch (SpecificException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @DeleteMapping("/profiles/{id}")
    public ResponseEntity<?> deleteProfile(@PathVariable Long id) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        try {
            voiceCloningService.deleteProfile(user, id);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Voice profile deleted successfully"
            ));
        } catch (SpecificException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/synthesize")
    public ResponseEntity<?> synthesizeSpeech(@Valid @RequestBody Map<String, String> request) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String text = request.get("text");
        if (text == null || text.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Text is required"));
        }

        String sentiment = request.get("sentiment");
        Double confidence = null;
        if (request.containsKey("confidence") && request.get("confidence") != null) {
            try {
                confidence = Double.parseDouble(String.valueOf(request.get("confidence")));
            } catch (SpecificException e) {
                // Ignore parsing errors
            }
        }

        try {
            byte[] audio = voiceCloningService.synthesizeSpeech(user, text, sentiment, confidence);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(audio);
        } catch (SpecificException e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "error", e.getMessage()
            ));
        }
    }
}