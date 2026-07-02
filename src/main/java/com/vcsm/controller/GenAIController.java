package com.vcsm.controller;

import com.vcsm.model.Complaint;
import com.vcsm.service.GenAIResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/genai")
@CrossOrigin(origins = "*")
public class GenAIController {

    @Autowired
    private GenAIResolver genAIResolver;

    @PostMapping("/resolve")
    public ResponseEntity<GenAIResolver.ResolutionResult> resolveComplaint(@Valid @RequestBody Complaint complaint) {
        GenAIResolver.ResolutionResult result = genAIResolver.resolveComplaint(complaint);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/summarize")
    public ResponseEntity<GenAIResolver.CallSummaryResult> summarizeCallSession(@Valid @RequestBody Map<String, String> request) {
        String transcript = request.get("transcript");
        String residentEmail = request.get("residentEmail");
        
        if (transcript == null || transcript.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        GenAIResolver.CallSummaryResult result = genAIResolver.summarizeCallSession(transcript, residentEmail);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "GenAI Resolver active");
        status.put("model", "GPT-4 Compatible");
        status.put("features", new String[]{
            "Auto-response generation",
            "Solution matching",
            "Multi-language support",
            "Tone personalization"
        });
        return ResponseEntity.ok(status);
    }
}
