package com.vcsm.controller;

import com.vcsm.bci.BCIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/bci")
@CrossOrigin(origins = "*")
public class BCIController {

    @Autowired
    private BCIService bciService;

    @PostMapping("/start")
    public ResponseEntity<BCIService.BCISession> startSession(@RequestParam String userId) {
        return ResponseEntity.ok(bciService.startSession(userId));
    }

    @PostMapping("/signal")
    public ResponseEntity<BCIService.BrainSignalResult> processSignal(
            @RequestParam String sessionId,
            @RequestBody double[] signalData) {
        return ResponseEntity.ok(bciService.processSignal(sessionId, signalData));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<BCIService.BCISession> getSession(@PathVariable String sessionId) {
        BCIService.BCISession session = bciService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getHistory(@PathVariable String userId) {
        return ResponseEntity.ok(bciService.getSignalHistory(userId));
    }

    @PostMapping("/end/{sessionId}")
    public ResponseEntity<Map<String, String>> endSession(@PathVariable String sessionId) {
        bciService.endSession(sessionId);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Session ended"));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(bciService.getBCIStats());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "BCI System active");
        status.put("features", new String[]{
            "EEG Signal Processing",
            "Thought Detection",
            "Mental State Analysis",
            "Real-time Processing",
            "Neurofeedback"
        });
        return ResponseEntity.ok(status);
    }
}