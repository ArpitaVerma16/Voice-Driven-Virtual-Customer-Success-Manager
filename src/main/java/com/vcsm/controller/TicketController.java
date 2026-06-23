package com.vcsm.controller;

import com.vcsm.model.Complaint;
import com.vcsm.service.SmartRouter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private SmartRouter smartRouter;

    @PostMapping("/classify")
    public ResponseEntity<SmartRouter.RoutingResult> classifyTicket(@RequestBody Complaint complaint) {
        SmartRouter.RoutingResult result = smartRouter.classifyAndRoute(complaint);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/auto-assign")
    public ResponseEntity<SmartRouter.RoutingResult> autoAssign(@RequestBody Complaint complaint) {
        SmartRouter.RoutingResult result = smartRouter.classifyAndRoute(complaint);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("status", "Ticket classification system running");
        stats.put("categories", new String[]{"NOISE", "MAINTENANCE", "SECURITY", "CLEANLINESS", "PARKING", "UTILITIES", "OTHER"});
        stats.put("urgencyLevels", new String[]{"LOW", "MEDIUM", "HIGH", "CRITICAL"});
        return ResponseEntity.ok(stats);
    }
}
