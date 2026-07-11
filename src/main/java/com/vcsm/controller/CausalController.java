package com.vcsm.controller;

import com.vcsm.ai.CausalEngine;
import com.vcsm.ai.CausalGraphBuilder;
import com.vcsm.ai.CounterfactualSimulator;
import com.vcsm.ai.RootCauseAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/causal")
@lombok.RequiredArgsConstructor
public class CausalController {

    private final CausalEngine causalEngine;

    private final RootCauseAnalyzer rootCauseAnalyzer;

    private final CounterfactualSimulator counterfactualSimulator;

    private final CausalGraphBuilder causalGraphBuilder;

    @GetMapping("/analyze")
    public ResponseEntity<CausalEngine.CausalAnalysis> analyze(@RequestParam String issue) {
        return ResponseEntity.ok(causalEngine.analyze(issue));
    }

    @GetMapping("/root-causes")
    public ResponseEntity<RootCauseAnalyzer.RootCauseAnalysis> getRootCauses(@RequestParam String issue) {
        return ResponseEntity.ok(rootCauseAnalyzer.findRootCauses(issue));
    }

    @GetMapping("/counterfactual")
    public ResponseEntity<CounterfactualSimulator.CounterfactualResult> simulateCounterfactual(
            @RequestParam String issue,
            @RequestParam String change,
            @RequestParam String changeValue) {
        return ResponseEntity.ok(counterfactualSimulator.simulate(issue, change, changeValue));
    }

    @GetMapping("/graph")
    public ResponseEntity<Map<String, Object>> getCausalGraph() {
        Map<String, Object> response = new HashMap<>();
        response.put("graph", causalGraphBuilder.getGraph());
        response.put("status", "Causal graph available");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "Causal AI System active");
        status.put("features", new String[]{
            "Root Cause Analysis",
            "Counterfactual Reasoning",
            "Causal Graph Building",
            "Intervention Recommendations",
            "Impact Analysis"
        });
        return ResponseEntity.ok(status);
    }
}
