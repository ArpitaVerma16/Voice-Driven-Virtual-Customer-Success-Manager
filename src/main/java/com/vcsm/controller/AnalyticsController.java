package com.vcsm.controller;

import com.vcsm.service.ComplaintService;
import com.vcsm.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
@lombok.RequiredArgsConstructor
public class AnalyticsController {



    private final ComplaintService complaintService;

    private final EventService eventService;

    private final com.vcsm.service.AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> analytics() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Long> cStats = complaintService.getComplaintStats();
        data.put("complaintStats", cStats);
        data.put("complaintsByCategory", complaintService.getComplaintsByCategory());
        data.put("totalEvents", (long) eventService.getAllEvents().size());
        data.put("activeEvents", (long) eventService.getActiveEvents().size());
        data.put("upcomingEvents", (long) eventService.getUpcomingEvents().size());
        long total = cStats.getOrDefault("total", 0L);
        long resolved = cStats.getOrDefault("resolved", 0L) + cStats.getOrDefault("closed", 0L);
        data.put("resolutionRate", total > 0 ? Math.round((double) resolved / total * 1000.0) / 10.0 : 0.0);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/daily-sentiment")
    public ResponseEntity<Map<String, Object>> getDailySentiment(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getDailySentiment(days));
    }

    @GetMapping("/top-intents")
    public ResponseEntity<Map<String, Long>> getTopIntents(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getTopIntents(days));
    }

    @GetMapping("/escalation-rate")
    public ResponseEntity<Map<String, Object>> getEscalationRate() {
        return ResponseEntity.ok(analyticsService.getEscalationRate());
    }
}