package com.vcsm.service;

import com.vcsm.model.AgentRequest;
import com.vcsm.service.agents.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MultiAgentOrchestrator {

    @Autowired
    private AgentRouter agentRouter;

    @Autowired
    private ComplaintAgent complaintAgent;

    @Autowired
    private EventAgent eventAgent;

    @Autowired
    private AnalyticsAgent analyticsAgent;

    @Autowired
    private StatusAgent statusAgent;

    @Autowired
    private HelpAgent helpAgent;

    public Map<String, Object> processRequest(AgentRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        // 1. Detect intents
        List<String> intents = agentRouter.detectIntents(request.getUserQuery());
        request.setIntents(intents);

        result.put("originalQuery", request.getUserQuery());
        result.put("detectedIntents", intents);

        // 2. Route to agents
        Map<String, Object> agentResults = new HashMap<>();
        List<Map<String, Object>> responses = new ArrayList<>();

        for (String intent : intents) {
            Map<String, Object> response = executeAgent(intent, request.getUserQuery(), request.getUserId());
            agentResults.put(intent, response);
            responses.add(response);
        }

        // 3. Merge responses
        String mergedResponse = mergeResponses(responses);
        result.put("mergedResponse", mergedResponse);
        result.put("agentResults", agentResults);
        result.put("success", true);

        return result;
    }

    private Map<String, Object> executeAgent(String intent, String query, Long userId) {
        switch (intent) {
            case "COMPLAINT":
                return complaintAgent.process(query, userId);
            case "EVENT":
                return eventAgent.process(query, userId);
            case "ANALYTICS":
                return analyticsAgent.process(query, userId);
            case "STATUS":
                return statusAgent.process(query, userId);
            case "HELP":
            default:
                return helpAgent.process(query, userId);
        }
    }

    private String mergeResponses(List<Map<String, Object>> responses) {
        if (responses.isEmpty()) {
            return "I didn't understand your request. Please try again.";
        }

        if (responses.size() == 1) {
            return (String) responses.get(0).getOrDefault("message", "Done!");
        }

        StringBuilder merged = new StringBuilder();
        for (int i = 0; i < responses.size(); i++) {
            Map<String, Object> response = responses.get(i);
            String message = (String) response.getOrDefault("message", "");
            if (!message.isEmpty()) {
                if (i > 0) merged.append("\n\n");
                merged.append("🔹 ").append(message);
            }
        }

        return merged.toString();
    }
}