package com.vcsm.service;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AgentRouter {

    private static final Map<String, List<String>> INTENT_KEYWORDS = new HashMap<>();

    static {
        INTENT_KEYWORDS.put("COMPLAINT", Arrays.asList(
            "complaint", "issue", "problem", "noise", "maintenance", 
            "security", "parking", "cleanliness", "utilities"
        ));
        INTENT_KEYWORDS.put("EVENT", Arrays.asList(
            "event", "register", "upcoming", "sports", "cultural", 
            "activity", "workshop", "seminar"
        ));
        INTENT_KEYWORDS.put("ANALYTICS", Arrays.asList(
            "analytics", "stats", "statistics", "how many", 
            "count", "summary", "report", "data"
        ));
        INTENT_KEYWORDS.put("STATUS", Arrays.asList(
            "status", "track", "check", "update", "progress"
        ));
        INTENT_KEYWORDS.put("HELP", Arrays.asList(
            "help", "support", "assistance", "guide", "tutorial"
        ));
    }

    public List<String> detectIntents(String query) {
        String lowerQuery = query.toLowerCase();
        List<String> detectedIntents = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerQuery.contains(keyword)) {
                    detectedIntents.add(entry.getKey());
                    break;
                }
            }
        }

        // If no intent detected, default to HELP
        if (detectedIntents.isEmpty()) {
            detectedIntents.add("HELP");
        }

        return detectedIntents;
    }

    public Map<String, Object> routeToAgent(String intent, String query, Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("intent", intent);
        response.put("query", query);
        response.put("userId", userId);

        switch (intent) {
            case "COMPLAINT":
                response.put("agent", "ComplaintAgent");
                response.put("action", "processComplaint");
                break;
            case "EVENT":
                response.put("agent", "EventAgent");
                response.put("action", "processEvent");
                break;
            case "ANALYTICS":
                response.put("agent", "AnalyticsAgent");
                response.put("action", "processAnalytics");
                break;
            case "STATUS":
                response.put("agent", "StatusAgent");
                response.put("action", "processStatus");
                break;
            case "HELP":
            default:
                response.put("agent", "HelpAgent");
                response.put("action", "processHelp");
                break;
        }

        return response;
    }

    public List<Map<String, Object>> routeToMultipleAgents(List<String> intents, String query, Long userId) {
        return intents.stream()
            .map(intent -> routeToAgent(intent, query, userId))
            .collect(Collectors.toList());
    }
}