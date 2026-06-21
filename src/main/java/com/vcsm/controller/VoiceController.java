package com.vcsm.controller;

import com.vcsm.service.LanguageDetectionService;
import com.vcsm.service.HindiCommandMapper;
import com.vcsm.model.VoiceCommand;
import com.vcsm.service.OmnidimService;
import com.vcsm.service.SentimentAnalysisService;
import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.vcsm.dto.ErrorResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "*")
public class VoiceController {

    @Autowired
    private OmnidimService omnidimService;
    
    @Autowired
    private SentimentAnalysisService sentimentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LanguageDetectionService languageDetectionService;

    @Autowired
    private HindiCommandMapper hindiCommandMapper;

    @Autowired
    private com.vcsm.service.EventRegistrationService eventRegistrationService;

    @PostMapping("/command")
    public ResponseEntity<?> command(@RequestBody Map<String, String> body) {
        String transcript = body.get("transcript");
        
        if (transcript == null || transcript.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transcript required", "success", false));
        }
        
        // Detect language
        String language = languageDetectionService.detectLanguage(transcript);
        
        Map<String, Object> response = new HashMap<>();
        response.put("originalText", transcript);
        response.put("detectedLanguage", language);
        
        // Process based on language
        if (language.equals("hi")) {
            // Hindi command
            String action = hindiCommandMapper.mapCommand(transcript);
            if (action != null) {
                response.put("action", action);
                
                String extraData = null;
                if ("cancel_registration".equals(action)) {
                    try {
                        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                        User user = null;
                        if (auth != null && auth.getName() != null) {
                            user = userRepository.findByEmail(auth.getName()).orElse(null);
                        }
                        if (user == null) {
                            user = userRepository.findById(1L).orElse(null);
                        }
                        
                        if (user != null) {
                            List<com.vcsm.model.Event> userEvents = eventRegistrationService.getUserEvents(user);
                            com.vcsm.model.Event matchedEvent = null;
                            if (!userEvents.isEmpty()) {
                                String lowerTranscript = transcript.toLowerCase();
                                for (com.vcsm.model.Event e : userEvents) {
                                    if (lowerTranscript.contains(e.getName().toLowerCase())) {
                                        matchedEvent = e;
                                        break;
                                    }
                                }
                                if (matchedEvent == null && userEvents.size() == 1) {
                                    matchedEvent = userEvents.get(0);
                                }
                            }
                            
                            if (matchedEvent != null) {
                                eventRegistrationService.cancelRegistration(matchedEvent, user);
                                extraData = ": " + matchedEvent.getName();
                            } else {
                                extraData = " (इवेंट नहीं मिला)";
                            }
                        }
                    } catch (Exception e) {
                        extraData = " (त्रुटि: " + e.getMessage() + ")";
                    }
                }
                
                response.put("response", hindiCommandMapper.getResponse(action, extraData));
                response.put("success", true);
            } else {
                response.put("action", "unknown");
                response.put("response", hindiCommandMapper.getDefaultResponse());
                response.put("success", false);
            }
        } else {
            // English command - use existing logic
            Map<String, Object> englishResponse = omnidimService.processVoiceCommand(transcript);
            response.putAll(englishResponse);
            response.put("success", true);
        }
        
        // Analyze sentiment dynamically from authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(401, "Unauthorized", "Authentication required"));
        }

        User user = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + auth.getName()));

        Long userId = user.getId();
        sentimentService.analyzeAndProcess(userId, transcript);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<VoiceCommand>> history() {
        return ResponseEntity.ok(omnidimService.getRecentCommands());
    }
}