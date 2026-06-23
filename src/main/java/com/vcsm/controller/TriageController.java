package com.vcsm.controller;

import com.vcsm.model.Complaint;
import com.vcsm.model.TriageRequest;
import com.vcsm.service.TriageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/triage")
public class TriageController {

    @Autowired
    private TriageService triageService;

    @PostMapping("/classify")
    public ResponseEntity<TriageRequest> classifyComplaint(@RequestBody Complaint complaint) {
        TriageRequest result = triageService.triageComplaint(complaint);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{complaintId}")
    public ResponseEntity<TriageRequest> getTriageResult(@PathVariable Long complaintId) {
        TriageRequest result = triageService.getTriageResult(complaintId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getTriageStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("status", "Triage system running");
        stats.put("categories", List.of("NOISE", "MAINTENANCE", "SECURITY", "CLEANLINESS", "PARKING", "UTILITIES", "OTHER"));
        stats.put("severityLevels", List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        return ResponseEntity.ok(stats);
    }
}
