package com.vcsm.controller;

import com.vcsm.security.AdversarialDefense;
import com.vcsm.security.AttackDetector;
import com.vcsm.security.RobustnessVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/defense")
@CrossOrigin(origins = "*")
@lombok.RequiredArgsConstructor
public class SecurityDefenseController {

    private final AdversarialDefense adversarialDefense;

    private final AttackDetector attackDetector;

    private final RobustnessVerifier robustnessVerifier;

    @PostMapping("/detect")
    public ResponseEntity<AdversarialDefense.DefenseResult> detectAdversarial(
            @RequestParam String input,
            @RequestParam(defaultValue = "TEXT") String modelType,
            @RequestParam(required = false) String userId) {

        AdversarialDefense.DefenseResult result = adversarialDefense.detectAdversarial(input, modelType);

        // Log attack if detected
        if (result.isAdversarial() && userId != null) {
            attackDetector.detectAttack(userId, result.getAttackType(), input);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/verify-robustness")
    public ResponseEntity<RobustnessVerifier.RobustnessReport> verifyRobustness(
            @RequestParam String modelName) {
        return ResponseEntity.ok(robustnessVerifier.verifyRobustness(modelName));
    }

    @GetMapping("/attack-stats")
    public ResponseEntity<Map<String, Object>> getAttackStats() {
        return ResponseEntity.ok(attackDetector.getAttackStats());
    }

    @GetMapping("/attack-logs/{userId}")
    public ResponseEntity<?> getAttackLogs(@PathVariable String userId) {
        return ResponseEntity.ok(attackDetector.getAttackLogs(userId));
    }

    @PostMapping("/reset-attacks/{userId}")
    public ResponseEntity<Map<String, String>> resetAttacks(@PathVariable String userId) {
        attackDetector.resetAttackCount(userId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Attack count reset"));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "Adversarial Defense System active");
        status.put("features", new String[]{
            "Adversarial Detection",
            "Attack Classification",
            "Robustness Verification",
            "Defensive Distillation"
        });
        return ResponseEntity.ok(status);
    }
}