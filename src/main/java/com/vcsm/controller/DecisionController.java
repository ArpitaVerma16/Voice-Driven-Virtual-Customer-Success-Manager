package com.vcsm.controller;

import com.vcsm.model.Complaint;
import com.vcsm.model.Decision;
import com.vcsm.service.DecisionEngine;
import com.vcsm.service.ExplainabilityService;
import com.vcsm.service.ReinforcementLearningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/decision")
@CrossOrigin(origins = "*")
public class DecisionController {

    @Autowired
    private DecisionEngine decisionEngine;

    @Autowired
    private ExplainabilityService explainabilityService;

    @Autowired
    private ReinforcementLearningService rlService;

    @PostMapping("/make")
    public ResponseEntity<Decision> makeDecision(@RequestBody Complaint complaint) {
        Decision decision = decisionEngine.makeDecision(complaint);
        return ResponseEntity.ok(decision);
    }

    @GetMapping("/explain")
    public ResponseEntity<Map<String, Object>> explain(@RequestParam String decisionType) {
        Map<String, Object> factors = new HashMap<>();
        factors.put("urgency", 75);
        factors.put("category", "MAINTENANCE");
        factors.put("recurrence", 3);
        factors.put("frustrated", true);
        
        ExplainabilityService.DecisionExplanation explanation = 
            explainabilityService.explain(decisionType, factors);
        
        Map<String, Object> response = new HashMap<>();
        response.put("decisionType", decisionType);
        response.put("explanation", explanation.getReasons());
        response.put("summary", explainabilityService.getSummary(explanation));
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/learn/stats")
    public ResponseEntity<Map<String, Object>> getLearningStats() {
        return ResponseEntity.ok(rlService.getLearningStats());
    }
}