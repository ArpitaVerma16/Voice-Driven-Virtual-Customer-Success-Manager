package com.vcsm.controller;

import com.vcsm.model.VoiceCommand;
import com.vcsm.service.OmnidimService;
import com.vcsm.service.SentimentAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/command")
    public ResponseEntity<Map<String, Object>> command(@RequestBody Map<String, String> body) {
        String transcript = body.get("transcript");
        
        if (transcript == null || transcript.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transcript required", "success", false));
        }
        
        // Process voice command
        Map<String, Object> response = omnidimService.processVoiceCommand(transcript);
        
        // Analyze sentiment (using dummy userId 1 for now, you can get from session)
        Long userId = 1L;
        sentimentService.analyzeAndProcess(userId, transcript);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<VoiceCommand>> history() {
        return ResponseEntity.ok(omnidimService.getRecentCommands());
    }
}