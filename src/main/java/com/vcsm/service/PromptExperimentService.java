package com.vcsm.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Random;

@Service
public class PromptExperimentService {

    private final Map<String, String> userGroups = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public String assignGroup(String userId) {
        return userGroups.computeIfAbsent(userId, k -> random.nextBoolean() ? "A" : "B");
    }

    public String getGreeting(String userId) {
        String group = assignGroup(userId);
        if ("A".equals(group)) {
            return "Hello, I am your Virtual Community Manager. How can I assist you today?";
        } else {
            return "Hey there! I'm your digital assistant. What's on your mind?";
        }
    }
    
    public Map<String, Object> getExperimentStats() {
        // Mocked analytics for the dashboard
        return Map.of(
            "Group_A", Map.of("avgSentiment", 0.85, "resolutionRate", "92%", "samples", 145),
            "Group_B", Map.of("avgSentiment", 0.76, "resolutionRate", "88%", "samples", 132)
        );
    }
}
