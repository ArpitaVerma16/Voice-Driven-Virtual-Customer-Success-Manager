package com.vcsm.controller;

import com.vcsm.quantum.QuantumFederatedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quantum-fed")
@CrossOrigin(origins = "*")
public class QuantumFederatedController {

    @Autowired
    private QuantumFederatedService quantumFederatedService;

    @PostMapping("/start")
    public ResponseEntity<QuantumFederatedService.FederatedRound> startRound() {
        return ResponseEntity.ok(quantumFederatedService.startRound());
    }

    @PostMapping("/participate")
    public ResponseEntity<QuantumFederatedService.ClientParticipation> participate(
            @RequestParam String roundId,
            @RequestParam String clientId,
            @Valid @RequestBody ClientUpdateRequest request) {
        return ResponseEntity.ok(quantumFederatedService.participate(
            roundId, clientId, request.getWeights(), request.getDataSize()
        ));
    }

    @PostMapping("/aggregate/{roundId}")
    public ResponseEntity<QuantumFederatedService.QuantumAggregationResult> aggregate(@PathVariable String roundId) {
        return ResponseEntity.ok(quantumFederatedService.aggregate(roundId));
    }

    @PostMapping("/predict")
    public ResponseEntity<QuantumFederatedService.QuantumInferenceResult> predict(@Valid @RequestBody double[] input) {
        return ResponseEntity.ok(quantumFederatedService.quantumPredict(input));
    }

    @GetMapping("/rounds")
    public ResponseEntity<List<QuantumFederatedService.FederatedRound>> getRounds() {
        return ResponseEntity.ok(quantumFederatedService.getAllRounds());
    }

    @GetMapping("/rounds/{roundId}")
    public ResponseEntity<QuantumFederatedService.FederatedRound> getRound(@PathVariable String roundId) {
        return ResponseEntity.ok(quantumFederatedService.getRound(roundId));
    }

    @GetMapping("/model")
    public ResponseEntity<Map<String, Object>> getGlobalModel() {
        double[] model = quantumFederatedService.getGlobalModel();
        Map<String, Object> response = new HashMap<>();
        response.put("model", model);
        response.put("size", model != null ? model.length : 0);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(quantumFederatedService.getStats());
    }

    public static class ClientUpdateRequest {
        private double[] weights;
        private int dataSize;

        public double[] getWeights() { return weights; }
        public void setWeights(double[] weights) { this.weights = weights; }
        public int getDataSize() { return dataSize; }
        public void setDataSize(int dataSize) { this.dataSize = dataSize; }
    }
}