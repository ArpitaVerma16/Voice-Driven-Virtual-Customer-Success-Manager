package com.vcsm.controller;

import com.vcsm.healing.SelfHealingEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/healing")
@lombok.RequiredArgsConstructor
public class SelfHealingController {

    private final SelfHealingEngine selfHealingEngine;

    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> runHealingCycle() {
        selfHealingEngine.runHealingCycle();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Healing cycle executed"));
    }

    @GetMapping("/reports")
    public ResponseEntity<List<SelfHealingEngine.HealingReport>> getReports() {
        return ResponseEntity.ok(selfHealingEngine.getAllHealingReports());
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(selfHealingEngine.getHealingStats());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "Self-Healing System active");
        status.put("features", new String[]{
            "Anomaly Detection",
            "Auto Recovery",
            "Predictive Failure Detection",
            "Circuit Breaker"
        });
        return ResponseEntity.ok(status);
    }
}
