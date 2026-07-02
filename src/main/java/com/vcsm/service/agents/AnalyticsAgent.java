package com.vcsm.service.agents;

import com.vcsm.service.ComplaintService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@lombok.RequiredArgsConstructor
public class AnalyticsAgent {

    private final ComplaintService complaintService;

    public Map<String, Object> process(String query, Long userId) {
        Map<String, Object> response = new HashMap<>();

        Map<String, Long> stats = complaintService.getComplaintStats();

        response.put("success", true);
        response.put("action", "analytics");
        response.put("message", "Here are the current statistics:");
        response.put("stats", stats);

        if (query.toLowerCase().contains("resolution")) {
            long total = stats.getOrDefault("total", 0L);
            long resolved = stats.getOrDefault("resolved", 0L) + stats.getOrDefault("closed", 0L);
            double rate = total > 0 ? (resolved * 100.0 / total) : 0;
            response.put("resolutionRate", Math.round(rate));
            response.put("message", "Resolution rate: " + Math.round(rate) + "%");
        }

        return response;
    }
}