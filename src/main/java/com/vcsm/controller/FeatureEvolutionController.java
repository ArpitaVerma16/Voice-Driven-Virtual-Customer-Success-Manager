package com.vcsm.controller;

import com.vcsm.service.ABTestingService;
import com.vcsm.service.FeatureAnalyzer;
import com.vcsm.service.FeatureEvolutionEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evolution")
public class FeatureEvolutionController {

    @Autowired
    private FeatureAnalyzer featureAnalyzer;

    @Autowired
    private ABTestingService abTestingService;

    @Autowired
    private FeatureEvolutionEngine evolutionEngine;

    @GetMapping("/analyze")
    public ResponseEntity<FeatureAnalyzer.FeatureAnalysis> analyzeFeatures() {
        return ResponseEntity.ok(featureAnalyzer.analyzeFeatures());
    }

    @GetMapping("/scores")
    public ResponseEntity<Map<String, Double>> getScores() {
        return ResponseEntity.ok(evolutionEngine.getFeatureScores());
    }

    @GetMapping("/top")
    public ResponseEntity<List<String>> getTopFeatures(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(evolutionEngine.getTopFeatures(limit));
    }

    @GetMapping("/stale")
    public ResponseEntity<List<String>> getStaleFeatures() {
        return ResponseEntity.ok(evolutionEngine.getStaleFeatures());
    }

    @PostMapping("/abtest/create")
    public ResponseEntity<Map<String, String>> createABTest(
            @RequestParam String testName,
            @RequestParam List<String> variants) {
        
        abTestingService.createTest(testName, variants.toArray(new String[0]));
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Test '" + testName + "' created with " + variants.size() + " variants"
        ));
    }

    @GetMapping("/abtest/results")
    public ResponseEntity<Map<String, Object>> getABTestResults(@RequestParam String testName) {
        return ResponseEntity.ok(abTestingService.getTestResults(testName));
    }

    @PostMapping("/evolve")
    public ResponseEntity<Map<String, String>> runEvolution() {
        evolutionEngine.runEvolutionCycle();
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Evolution cycle completed successfully"
        ));
    }
}