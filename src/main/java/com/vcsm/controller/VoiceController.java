package com.vcsm.controller;

import com.vcsm.service.LanguageDetectionService;
import com.vcsm.service.HindiCommandMapper;
import com.vcsm.model.VoiceCommand;
import com.vcsm.service.OmnidimService;
import com.vcsm.service.SentimentAnalysisService;
import com.vcsm.service.IvrService;
import com.vcsm.dto.IvrNode;
import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.vcsm.dto.ErrorResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.vcsm.dto.ErrorResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import com.vcsm.dto.VoiceCommandRequest;

@RestController
@RequestMapping("/api/voice")
@lombok.RequiredArgsConstructor
public class VoiceController {

    private final OmnidimService omnidimService;
    
    private final SentimentAnalysisService sentimentService;

    private final UserRepository userRepository;

    private final LanguageDetectionService languageDetectionService;

    private final HindiCommandMapper hindiCommandMapper;

    private final com.vcsm.service.EventRegistrationService eventRegistrationService;

    private final com.vcsm.service.PromptExperimentService promptExperimentService;

    @PostMapping("/command")
    public ResponseEntity<?> command(@Valid @RequestBody Map<String, String> body) {
        String transcript = body.get("transcript");
        
        if (transcript == null || transcript.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transcript required", "success", false));
        }
        
        // Authentication check FIRST
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", 401);
            error.put("error", "Unauthorized");
            error.put("message", "Authentication required");
            error.put("success", false);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        User user = userRepository.findByEmail(auth.getName())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + auth.getName()));
        
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
            // English command - route dynamically via active IVR JSON flow tree
            Map<String, Object> ivrResponse = ivrService.processInteraction(user.getEmail(), transcript);
            response.put("action", ivrResponse.get("action"));
            response.put("response", ivrResponse.get("prompt"));
            response.put("success", ivrResponse.get("success"));
            response.put("currentNodeId", ivrResponse.get("currentNodeId"));
        }
        
        // Analyze sentiment dynamically from authenticated user
        Long userId = user.getId();
        sentimentService.analyzeAndProcess(userId, transcript);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<VoiceCommand>> history(
            @RequestParam(required = false) Boolean success) {

        return ResponseEntity.ok(
                omnidimService.getRecentCommands(success)
        );
    }

    @GetMapping("/flow-config")
    public ResponseEntity<IvrNode> getFlowConfig() {
        return ResponseEntity.ok(ivrService.getActiveFlow());
    }

    @PostMapping("/flow-config")
    public ResponseEntity<Map<String, Object>> saveFlowConfig(@Valid @RequestBody Map<String, String> body) {
        String flowJson = body.get("flowJson");
        if (flowJson == null || flowJson.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "flowJson is required", "success", false));
        }
        ivrService.saveFlow(flowJson);
        return ResponseEntity.ok(Map.of("message", "IVR Flow Configuration updated successfully", "success", true));
    }

    @GetMapping("/greeting")
    public ResponseEntity<Map<String, String>> getGreeting() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.getName() != null) ? auth.getName() : "anonymous";
        String prompt = promptExperimentService.getGreeting(username);
        return ResponseEntity.ok(Map.of(
            "prompt", prompt, 
            "group", promptExperimentService.assignGroup(username)
        ));
    }

    @GetMapping("/experiment-stats")
    public ResponseEntity<Map<String, Object>> getExperimentStats() {
        return ResponseEntity.ok(promptExperimentService.getExperimentStats());
    }

    // Webhook endpoint for OmniDimension's Custom API Integration.
    // Configure this URL in the OmniDimension dashboard as a Custom API
    // action so the voice agent can call back into VCSM with the user's
    // transcript and receive a structured response to speak back.
    @PostMapping("/omnidim-webhook")
    public ResponseEntity<Map<String, Object>> handleOmnidimWebhook(@RequestBody Map<String, String> payload) {
        String transcript = payload.getOrDefault("transcript", "");
        if (transcript.isBlank()) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "response", "I didn't catch that. Could you repeat your request?"
            ));
        }
        return ResponseEntity.ok(omnidimService.processVoiceCommand(transcript));
    }
}
