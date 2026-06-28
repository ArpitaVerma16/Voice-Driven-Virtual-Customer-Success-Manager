package com.vcsm.controller;

import com.vcsm.dto.SentimentResult;
import com.vcsm.service.SentimentAnalysisServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sentiment")
@CrossOrigin(origins = "*")
public class SentimentAnalysisController {

    @Autowired
    private SentimentAnalysisServiceImpl sentimentService;

    @PostMapping("/analyze")
    public ResponseEntity<Map<String, Object>> analyzeTranscript(@RequestBody Map<String, String> request) {
        String transcript = request.get("transcript");

        if (transcript == null || transcript.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Transcript is required",
                "success", false
            ));
        }

        SentimentResult result = sentimentService.analyzeTranscript(transcript);

        Map<String, Object> response = new HashMap<>();
        response.put("sentiment", result.getSentiment());
        response.put("negativeScore", String.format("%.3f", result.getNegativeScore()));
        response.put("positiveScore", String.format("%.3f", result.getPositiveScore()));
        response.put("negativeKeywordCount", result.getNegativeKeywords());
        response.put("positiveKeywordCount", result.getPositiveKeywords());
        response.put("requiresEscalation", result.isRequiresEscalation());
        if (result.isRequiresEscalation()) {
            response.put("escalationReason", result.getEscalationReason());
        }
        response.put("success", true);

        return ResponseEntity.ok(response);
    }
}
