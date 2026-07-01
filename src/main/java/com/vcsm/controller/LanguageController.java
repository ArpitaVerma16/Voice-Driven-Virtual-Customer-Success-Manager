package com.vcsm.controller;

import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.HindiCommandMapper;
import com.vcsm.service.LanguageDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/language")
public class LanguageController {
    
    @Autowired
    private LanguageDetectionService languageDetectionService;
    
    @Autowired
    private HindiCommandMapper hindiCommandMapper;
    
    @Autowired
    private UserRepository userRepository;
    
    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detectLanguage(@Valid @RequestBody Map<String, String> request) {
        String text = request.get("text");
        String language = languageDetectionService.detectLanguage(text);
        
        Map<String, Object> response = new HashMap<>();
        response.put("language", language);
        response.put("text", text);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/set/{userId}")
    public ResponseEntity<Map<String, Object>> setLanguagePreference(
            @PathVariable Long userId,
            @Valid @RequestBody Map<String, String> request) {
        
        String language = request.get("language");
        Optional<User> userOpt = userRepository.findById(userId);
        
        Map<String, Object> response = new HashMap<>();
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Add language field to User entity if not exists
            user.setPreferredLanguage(language);
            userRepository.save(user);
            response.put("success", true);
            response.put("message", "Language preference set to " + language);
        } else {
            response.put("success", false);
            response.put("message", "User not found");
        }
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/process-hindi")
    public ResponseEntity<Map<String, Object>> processHindiCommand(@Valid @RequestBody Map<String, String> request) {
        String hindiText = request.get("text");
        String action = hindiCommandMapper.mapCommand(hindiText);
        
        Map<String, Object> response = new HashMap<>();
        response.put("originalText", hindiText);
        response.put("detectedLanguage", "hi");
        
        if (action != null) {
            response.put("action", action);
            response.put("response", hindiCommandMapper.getResponse(action, null));
            response.put("success", true);
        } else {
            response.put("action", "unknown");
            response.put("response", hindiCommandMapper.getDefaultResponse());
            response.put("success", false);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "Language controller is working!");
        response.put("example_hindi_command", "शिकायत दर्ज करो");
        response.put("example_response", hindiCommandMapper.getResponse("file_complaint", null));
        return ResponseEntity.ok(response);
    }
}