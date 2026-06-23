package com.vcsm.controller;

import com.vcsm.model.SentimentAnalysis;
import com.vcsm.model.VoiceVerificationRequest;
import com.vcsm.service.SentimentAnalysisService;
import com.vcsm.utils.SentimentClassifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {
    
    @Autowired
    private SentimentAnalysisService sentimentService;
    
    @Autowired
    private SentimentClassifier sentimentClassifier;
    
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeSentiment(@RequestBody VoiceVerificationRequest request) {
        SentimentAnalysis result = sentimentService.analyzeAndProcess(
            request.getUserId(), 
            request.getText() != null ? request.getText() : ""
        );
        
        Map<String, Object> response = new HashMap<>();
        if (result != null) {
            response.put("sentiment", result.getSentiment());
            response.put("confidence", result.getConfidence());
            response.put("wasEscalated", result.isWasEscalated());
            response.put("message", result.isWasEscalated() ? 
                "⚠️ Negative sentiment detected! High priority complaint created." : 
                "Sentiment analyzed successfully.");
        } else {
            response.put("error", "User not found");
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSentimentStats() {
        return ResponseEntity.ok(sentimentService.getSentimentStats());
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<SentimentAnalysis>> getRecentAnalyses() {
        return ResponseEntity.ok(sentimentService.getRecentAnalyses(50));
    }
    
    @GetMapping("/escalations/pending")
    public ResponseEntity<?> getPendingEscalations() {
        return ResponseEntity.ok(sentimentService.getPendingEscalations());
    }
    
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testSentiment(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        SentimentClassifier.SentimentResult result = sentimentClassifier.analyze(text);
        
        Map<String, Object> response = new HashMap<>();
        response.put("text", text);
        response.put("sentiment", result.getSentiment());
        response.put("confidence", result.getConfidence());
        response.put("shouldEscalate", sentimentClassifier.shouldEscalate(result.getSentiment()));
        
        return ResponseEntity.ok(response);
    }
}
