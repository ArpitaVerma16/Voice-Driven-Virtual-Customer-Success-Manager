package com.vcsm.controller;

import com.vcsm.twin.PredictiveDigitalTwin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/twin")
@CrossOrigin(origins = "*")
@lombok.RequiredArgsConstructor
public class TwinController {

    private final PredictiveDigitalTwin predictiveDigitalTwin;

    @PostMapping("/create")
    public ResponseEntity<PredictiveDigitalTwin.TwinInstance> createTwin(
            @RequestParam String twinId,
            @RequestParam String systemType) {
        return ResponseEntity.ok(predictiveDigitalTwin.createTwin(twinId, systemType));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, String>> syncData(
            @RequestParam String twinId,
            @Valid @RequestBody SyncRequest request) {
        predictiveDigitalTwin.syncData(twinId, request.getMetrics(), request.getState());
        return ResponseEntity.ok(Map.of("status", "success", "message", "Data synced"));
    }

    @GetMapping("/predict/{twinId}")
    public ResponseEntity<PredictiveDigitalTwin.PredictionResult> predict(
            @PathVariable String twinId,
            @RequestParam(defaultValue = "5") int timeHorizon) {
        return ResponseEntity.ok(predictiveDigitalTwin.predictFuture(twinId, timeHorizon));
    }

    @GetMapping("/anomaly/{twinId}")
    public ResponseEntity<PredictiveDigitalTwin.AnomalyPrediction> predictAnomalies(
            @PathVariable String twinId) {
        return ResponseEntity.ok(predictiveDigitalTwin.predictAnomalies(twinId));
    }

    @GetMapping("/forecast/{twinId}")
    public ResponseEntity<PredictiveDigitalTwin.ResourceForecast> forecastResources(
            @PathVariable String twinId,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(predictiveDigitalTwin.forecastResources(twinId, days));
    }

    @GetMapping("/{twinId}")
    public ResponseEntity<PredictiveDigitalTwin.TwinInstance> getTwin(@PathVariable String twinId) {
        return ResponseEntity.ok(predictiveDigitalTwin.getTwin(twinId));
    }

    @GetMapping
    public ResponseEntity<List<PredictiveDigitalTwin.TwinInstance>> getAllTwins() {
        return ResponseEntity.ok(predictiveDigitalTwin.getAllTwins());
    }

    @GetMapping("/history/{twinId}")
    public ResponseEntity<List<PredictiveDigitalTwin.HistoricalData>> getHistory(@PathVariable String twinId) {
        return ResponseEntity.ok(predictiveDigitalTwin.getHistory(twinId));
    }

    @DeleteMapping("/{twinId}")
    public ResponseEntity<Map<String, String>> deleteTwin(@PathVariable String twinId) {
        predictiveDigitalTwin.deleteTwin(twinId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Twin deleted"));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(predictiveDigitalTwin.getTwinStats());
    }

    public static class SyncRequest {
        private double[] metrics;
        private String state;

        public double[] getMetrics() { return metrics; }
        public void setMetrics(double[] metrics) { this.metrics = metrics; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
    }
}