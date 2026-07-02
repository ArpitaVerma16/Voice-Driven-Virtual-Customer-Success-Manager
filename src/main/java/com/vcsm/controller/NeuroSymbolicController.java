package com.vcsm.controller;

import com.vcsm.ai.NeuroSymbolicEngine;
import com.vcsm.ai.RuleExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/neuro")
@CrossOrigin(origins = "*")
@lombok.RequiredArgsConstructor
public class NeuroSymbolicController {

    private final NeuroSymbolicEngine neuroSymbolicEngine;

    private final RuleExtractor ruleExtractor;

    @PostMapping("/reason")
    public ResponseEntity<NeuroSymbolicEngine.NeuroSymbolicResult> reason(
            @RequestParam String input,
            @RequestParam(defaultValue = "COMPLAINT") String domain) {
        return ResponseEntity.ok(neuroSymbolicEngine.process(input, domain));
    }

    @GetMapping("/rules")
    public ResponseEntity<List<RuleExtractor.Rule>> getRules(
            @RequestParam(required = false) String domain) {
        if (domain != null) {
            return ResponseEntity.ok(ruleExtractor.getRulesByDomain(domain));
        }
        return ResponseEntity.ok(ruleExtractor.extractRules(""));
    }

    @PostMapping("/rules/add")
    public ResponseEntity<Map<String, String>> addRule(@Valid @RequestBody RuleExtractor.Rule rule) {
        ruleExtractor.addRule(rule);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Rule added successfully"));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "Neuro-Symbolic AI System active");
        status.put("components", new String[]{
            "Neural Perception",
            "Symbolic Reasoning",
            "Rule Extraction",
            "Inference Engine",
            "Explainability"
        });
        return ResponseEntity.ok(status);
    }
}