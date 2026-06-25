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
@CrossOrigin(origins = "*")
@org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class AnalyticsController {



    @Autowired
    private ComplaintService complaintService;

    @Autowired
    private EventService eventService;

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
}