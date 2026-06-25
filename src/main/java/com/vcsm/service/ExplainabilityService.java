package com.vcsm.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ExplainabilityService {

    /**
     * Generate explanation for a decision
     */
    public DecisionExplanation explain(String decisionType, Map<String, Object> factors) {
        DecisionExplanation explanation = new DecisionExplanation(decisionType);

        switch (decisionType) {
            case "PRIORITIZE":
                explanation.addReason("Urgency score: " + factors.getOrDefault("urgency", 0));
                explanation.addReason("Category: " + factors.getOrDefault("category", "UNKNOWN"));
                explanation.addReason("Recurrence: " + factors.getOrDefault("recurrence", 0) + " times");
                break;
            case "ESCALATE":
                explanation.addReason("Resolution time exceeded: " + factors.getOrDefault("timeElapsed", 0) + " hours");
                explanation.addReason("Customer frustration detected: " + factors.getOrDefault("frustrated", false));
                explanation.addReason("Complexity score: " + factors.getOrDefault("complexity", 0));
                break;
            case "ASSIGN":
                explanation.addReason("Best match found: Admin " + factors.getOrDefault("admin", "N/A"));
                explanation.addReason("Match score: " + factors.getOrDefault("matchScore", 0) + "%");
                break;
            default:
                explanation.addReason("Decision based on predefined rules");
        }

        return explanation;
    }

    /**
     * Generate human-readable summary
     */
    public String getSummary(DecisionExplanation explanation) {
        StringBuilder summary = new StringBuilder();
        summary.append("📋 Decision: ").append(explanation.getDecisionType()).append("\n");
        summary.append("🔍 Reasons:\n");
        for (String reason : explanation.getReasons()) {
            summary.append("  • ").append(reason).append("\n");
        }
        return summary.toString();
    }

    public static class DecisionExplanation {
        private final String decisionType;
        private final List<String> reasons = new ArrayList<>();

        public DecisionExplanation(String decisionType) {
            this.decisionType = decisionType;
        }

        public void addReason(String reason) {
            reasons.add(reason);
        }

        public String getDecisionType() { return decisionType; }
        public List<String> getReasons() { return reasons; }
    }
}