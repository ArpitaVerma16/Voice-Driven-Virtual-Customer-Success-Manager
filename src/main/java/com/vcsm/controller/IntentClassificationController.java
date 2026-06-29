package com.vcsm.controller;

import com.vcsm.dto.IntentResult;
import com.vcsm.service.IntentClassificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/intents")
@CrossOrigin(origins = "*")
public class IntentClassificationController {

    @Autowired
    private IntentClassificationService intentClassificationService;

    @PostMapping("/classify")
    public ResponseEntity<Map<String, Object>> classify(@RequestBody Map<String, String> request) {
        String transcript = request.get("transcript");

        if (transcript == null || transcript.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Transcript is required",
                "success", false
            ));
        }

        IntentResult result = intentClassificationService.classify(transcript);

        Map<String, Object> response = new HashMap<>();
        response.put("classifiedIntent", result.getClassifiedIntent());
        response.put("confidence", String.format("%.3f", result.getConfidence()));
        response.put("isConfident", result.isConfident());
        response.put("summary", result.toSummary());
        response.put("allScores", result.getAllScores());
        response.put("success", true);

        return ResponseEntity.ok(response);
    }
}
