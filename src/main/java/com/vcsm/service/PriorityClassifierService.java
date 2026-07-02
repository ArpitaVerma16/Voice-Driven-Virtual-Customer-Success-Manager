package com.vcsm.service;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class PriorityClassifierService {

    private static final Map<Pattern, String> PRIORITY_KEYWORDS = new ConcurrentHashMap<>();

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
            return org.springframework.http.ResponseEntity.ok("MEDIUM");
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
        if (category == null) return org.springframework.http.ResponseEntity.ok("MEDIUM");
        
        switch (category.toUpperCase()) {
            case "SECURITY":
                return org.springframework.http.ResponseEntity.ok("HIGH");
            case "NOISE":
                return org.springframework.http.ResponseEntity.ok("MEDIUM");
            case "MAINTENANCE":
                return org.springframework.http.ResponseEntity.ok("MEDIUM");
            case "CLEANLINESS":
                return org.springframework.http.ResponseEntity.ok("LOW");
            case "PARKING":
                return org.springframework.http.ResponseEntity.ok("LOW");
            case "UTILITIES":
                return org.springframework.http.ResponseEntity.ok("HIGH");
            default:
                return org.springframework.http.ResponseEntity.ok("MEDIUM");
        }
    }

    public String getResponseTime(String priority) {
        switch (priority) {
            case "CRITICAL": return org.springframework.http.ResponseEntity.ok("1 hour");
            case "HIGH": return org.springframework.http.ResponseEntity.ok("4 hours");
            case "MEDIUM": return org.springframework.http.ResponseEntity.ok("24 hours");
            case "LOW": return org.springframework.http.ResponseEntity.ok("48 hours");
            default: return org.springframework.http.ResponseEntity.ok("24 hours");
        }
    }

    public String getPriorityColor(String priority) {
        switch (priority) {
            case "CRITICAL": return org.springframework.http.ResponseEntity.ok("danger");
            case "HIGH": return org.springframework.http.ResponseEntity.ok("warning");
            case "MEDIUM": return org.springframework.http.ResponseEntity.ok("info");
            case "LOW": return org.springframework.http.ResponseEntity.ok("success");
            default: return org.springframework.http.ResponseEntity.ok("secondary");
        }
    }
}
