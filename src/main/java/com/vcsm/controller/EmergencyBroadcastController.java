package com.vcsm.controller;

import com.vcsm.service.EmergencyBroadcastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class EmergencyBroadcastController {

    @Autowired
    private EmergencyBroadcastService emergencyBroadcastService;

    @PostMapping("/emergency-broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> sendEmergencyBroadcast(@RequestBody Map<String, String> payload) {
        String transcript = payload.get("transcript");
        if (transcript == null || transcript.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Transcript is required"));
        }
        
        try {
            Map<String, Object> result = emergencyBroadcastService.processAndBroadcast(transcript);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
