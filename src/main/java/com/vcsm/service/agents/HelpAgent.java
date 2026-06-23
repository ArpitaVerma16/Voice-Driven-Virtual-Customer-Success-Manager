package com.vcsm.service.agents;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class HelpAgent {

    public Map<String, Object> process(String query, Long userId) {
        Map<String, Object> response = new HashMap<>();

        response.put("success", true);
        response.put("action", "help");
        response.put("message", """
            I can help you with:
            
            📝 **Complaints** - File, track, and manage complaints
            📅 **Events** - Find and register for community events
            📊 **Analytics** - View statistics and reports
            📋 **Status** - Check complaint status
            
            Just tell me what you need help with!
            """);
        response.put("examples", new String[]{
            "I have a noise complaint",
            "Show upcoming events",
            "How many complaints are open?",
            "Check my complaint status"
        });

        return response;
    }
}