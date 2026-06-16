package com.vcsm.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class PriorityClassifierService {

    private static final Map<Pattern, String> PRIORITY_KEYWORDS = new HashMap<>();

    static {
        // CRITICAL - Immediate attention (1 hour response)
        PRIORITY_KEYWORDS.put(Pattern.compile("(?i).*(emergency|danger|fire|medical|injury|life.?threatening|critical|crisis).*"), "CRITICAL");
        
        // HIGH - Urgent (4 hours response)
        PRIORITY_KEYWORDS.put(Pattern.compile("(?i).*(security|break.?in|water.?leak|flood|gas.?leak|electrical|power.?outage|urgent|serious|dangerous).*"), "HIGH");
        
        // MEDIUM - Normal (24 hours response)
        PRIORITY_KEYWORDS.put(Pattern.compile("(?i).*(noise|parking|maintenance|cleaning|garbage|plumbing|hvac|heating|cooling).*"), "MEDIUM");
        
        // LOW - Minor (48 hours response)
        PRIORITY_KEYWORDS.put(Pattern.compile("(?i).*(suggestion|query|information|help|assistance|question|clarification|feedback).*"), "LOW");
    }

    public String classifyPriority(String description, String category) {
        if (description == null || description.isEmpty()) {
            return "MEDIUM";
        }

        // First check description for keywords
        for (Map.Entry<Pattern, String> entry : PRIORITY_KEYWORDS.entrySet()) {
            if (entry.getKey().matcher(description).matches()) {
                return entry.getValue();
            }
        }

        // Default based on category
        return getDefaultPriorityByCategory(category);
    }

    private String getDefaultPriorityByCategory(String category) {
        if (category == null) return "MEDIUM";
        
        switch (category.toUpperCase()) {
            case "SECURITY":
                return "HIGH";
            case "NOISE":
                return "MEDIUM";
            case "MAINTENANCE":
                return "MEDIUM";
            case "CLEANLINESS":
                return "LOW";
            case "PARKING":
                return "LOW";
            case "UTILITIES":
                return "HIGH";
            default:
                return "MEDIUM";
        }
    }

    public String getResponseTime(String priority) {
        switch (priority) {
            case "CRITICAL": return "1 hour";
            case "HIGH": return "4 hours";
            case "MEDIUM": return "24 hours";
            case "LOW": return "48 hours";
            default: return "24 hours";
        }
    }

    public String getPriorityColor(String priority) {
        switch (priority) {
            case "CRITICAL": return "danger";
            case "HIGH": return "warning";
            case "MEDIUM": return "info";
            case "LOW": return "success";
            default: return "secondary";
        }
    }
}