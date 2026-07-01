package com.vcsm.controller;

import com.vcsm.model.ModelVersion;
import com.vcsm.service.DriftDetector;
import com.vcsm.service.ModelEvolutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/evolution")
@CrossOrigin(origins = "*")
public class ModelEvolutionController {

    @Autowired
    private ModelEvolutionService modelEvolutionService;

    @Autowired
    private DriftDetector driftDetector;

    @PostMapping("/train")
    public ResponseEntity<ModelVersion> trainModel(@RequestParam String modelName) {
        return ResponseEntity.ok(modelEvolutionService.trainModel(modelName));
    }

    @GetMapping("/history/{modelName}")
    public ResponseEntity<List<ModelVersion>> getHistory(@PathVariable String modelName) {
        return ResponseEntity.ok(modelEvolutionService.getEvolutionHistory(modelName));
    }

    @GetMapping("/active/{modelName}")
    public ResponseEntity<ModelVersion> getActiveModel(@PathVariable String modelName) {
        ModelVersion model = modelEvolutionService.getActiveModel(modelName);
        if (model == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(model);
    }

    @PostMapping("/rollback/{modelName}")
    public ResponseEntity<ModelVersion> rollback(@PathVariable String modelName) {
        return ResponseEntity.ok(modelEvolutionService.rollback(modelName));
    }

    @GetMapping("/drift")
    public ResponseEntity<DriftDetector.DriftResult> checkDrift() {
        return ResponseEntity.ok(driftDetector.detectDrift());
    }

    @PostMapping("/cycle")
    public ResponseEntity<Map<String, String>> runEvolutionCycle() {
        modelEvolutionService.runEvolutionCycle();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Evolution cycle triggered"));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(modelEvolutionService.getEvolutionStats());
    }
}