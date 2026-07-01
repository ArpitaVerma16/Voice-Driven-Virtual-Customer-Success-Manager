package com.vcsm.controller;

import com.vcsm.service.PredictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predict")
public class PredictionController {

    @Autowired
    private PredictionService predictionService;

    @GetMapping("/complaints")
    public ResponseEntity<Map<String, Object>> predictComplaints(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(predictionService.predictComplaints(days));
    }

    @PostMapping("/event/{eventId}")
    public ResponseEntity<Map<String, Object>> predictEvent(
            @PathVariable Long eventId,
            @Valid @RequestBody(required = false) List<Map<String, Object>> historicalData) {
        return ResponseEntity.ok(predictionService.predictEventAttendance(eventId, historicalData));
    }

    @PostMapping("/sentiment")
    public ResponseEntity<Map<String, Object>> predictSentiment(
            @Valid @RequestBody(required = false) List<Map<String, Object>> historicalSentiment) {
        return ResponseEntity.ok(predictionService.predictSentiment(historicalSentiment));
    }

    @GetMapping("/peak-times")
    public ResponseEntity<Map<String, Object>> getPeakTimes() {
        return ResponseEntity.ok(predictionService.getPeakTimes());
    }
}