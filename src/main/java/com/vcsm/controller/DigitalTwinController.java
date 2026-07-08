package com.vcsm.controller;

import com.vcsm.model.DigitalTwin;
import com.vcsm.service.DigitalTwinService;
import com.vcsm.service.SimulationEngine.SimulationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/twin")
@lombok.RequiredArgsConstructor
public class DigitalTwinController {

    private final DigitalTwinService digitalTwinService;

    @PostMapping
    public ResponseEntity<DigitalTwin> createTwin(@Valid @RequestBody DigitalTwin twin) {
        return ResponseEntity.ok(digitalTwinService.createTwin(twin));
    }

    @PostMapping("/sync/{id}")
    public ResponseEntity<DigitalTwin> syncTwin(@PathVariable Long id) {
        return ResponseEntity.ok(digitalTwinService.syncTwin(id));
    }

    @PostMapping("/simulate/{id}")
    public ResponseEntity<SimulationResult> simulate(
            @PathVariable Long id,
            @RequestParam(defaultValue = "LOAD_TEST") String scenarioType,
            @RequestParam(defaultValue = "1") int multiplier) {
        
        return ResponseEntity.ok(digitalTwinService.runSimulation(id, scenarioType, multiplier));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DigitalTwin> getTwin(@PathVariable Long id) {
        DigitalTwin twin = digitalTwinService.getTwin(id);
        if (twin == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(twin);
    }

    @GetMapping
    public ResponseEntity<List<DigitalTwin>> getAllTwins() {
        return ResponseEntity.ok(digitalTwinService.getAllTwins());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTwin(@PathVariable Long id) {
        digitalTwinService.deleteTwin(id);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Digital twin deleted"));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(digitalTwinService.getTwinStats());
    }

    @GetMapping("/scenarios")
    public ResponseEntity<List<String>> getScenarios() {
        return ResponseEntity.ok(List.of(
            "LOAD_TEST - Stress test with increased load",
            "FAILURE_TEST - Simulate system failures",
            "CAPACITY_TEST - Test system capacity limits"
        ));
    }
}
