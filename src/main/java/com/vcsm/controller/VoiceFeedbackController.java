package com.vcsm.controller;

import com.vcsm.model.VoiceFeedback;
import com.vcsm.service.VoiceFeedbackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/voice/feedback")
@lombok.RequiredArgsConstructor
public class VoiceFeedbackController {
    
    private final VoiceFeedbackService voiceFeedbackService;
    
    @PostMapping
    public ResponseEntity<?> submitFeedback(
            @RequestParam Long commandId,
            @RequestParam Long userId,
            @RequestParam String feedback,
            @RequestParam(required = false) String comment) {
        
        try {
            VoiceFeedback result = voiceFeedbackService.submitFeedback(commandId, userId, feedback, comment);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Feedback submitted successfully",
                "feedback", result
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(voiceFeedbackService.getFeedbackStats());
    }
}