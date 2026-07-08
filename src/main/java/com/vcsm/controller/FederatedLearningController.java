package com.vcsm.controller;

import com.vcsm.ai.FederatedLearningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/federated")
@lombok.RequiredArgsConstructor
public class FederatedLearningController {

    private final FederatedLearningService federatedLearningService;

    @PostMapping("/start")
    public ResponseEntity<FederatedLearningService.FederatedRound> startRound() {
        return ResponseEntity.ok(federatedLearningService.startRound());
    }

    @PostMapping("/participate")
    public ResponseEntity<FederatedLearningService.ClientParticipation> participate(
            @RequestParam String roundId,
            @RequestParam String clientId,
            @Valid @RequestBody FederatedData data) {
        return ResponseEntity.ok(federatedLearningService.participate(
            roundId, clientId, data.getFeatures(), data.getLabels()
        ));
    }

    @PostMapping("/aggregate/{roundId}")
    public ResponseEntity<FederatedLearningService.AggregatedGlobalModel> aggregate(@PathVariable String roundId) {
        return ResponseEntity.ok(federatedLearningService.aggregate(roundId));
    }

    @GetMapping("/rounds")
    public ResponseEntity<List<FederatedLearningService.FederatedRound>> getRounds() {
        return ResponseEntity.ok(federatedLearningService.getAllRounds());
    }

    @GetMapping("/rounds/{roundId}")
    public ResponseEntity<FederatedLearningService.FederatedRound> getRound(@PathVariable String roundId) {
        return ResponseEntity.ok(federatedLearningService.getRound(roundId));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "Federated Learning System active");
        status.put("features", new String[]{
            "Local Model Training",
            "Secure Aggregation",
            "Differential Privacy",
            "Privacy Budget Tracking",
            "Auto-round Scheduling"
        });
        return ResponseEntity.ok(status);
    }

    public static class FederatedData {
        private List<double[]> features;
        private List<Double> labels;

        public List<double[]> getFeatures() { return features; }
        public void setFeatures(List<double[]> features) { this.features = features; }
        public List<Double> getLabels() { return labels; }
        public void setLabels(List<Double> labels) { this.labels = labels; }
    }
}
