package com.vcsm.ai;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class InferenceEngine {

    private final Map<String, Object> knowledgeBase = new HashMap<>();

    public InferenceEngine() {
        initializeKnowledgeBase();
    }

    private void initializeKnowledgeBase() {
        knowledgeBase.put("priority_levels", Arrays.asList("LOW", "MEDIUM", "HIGH", "CRITICAL"));
        knowledgeBase.put("categories", Arrays.asList("NOISE", "MAINTENANCE", "SECURITY", "PARKING", "UTILITIES", "OTHER"));
        knowledgeBase.put("resolution_times", Map.of(
            "LOW", "48h",
            "MEDIUM", "24h",
            "HIGH", "4h",
            "CRITICAL", "1h"
        ));
    }

    public InferenceResult reason(Map<String, Object> facts, List<RuleExtractor.Rule> rules) {
        List<String> inferences = new ArrayList<>();
        Map<String, Object> conclusions = new HashMap<>();

        for (RuleExtractor.Rule rule : rules) {
            boolean conditionsMet = true;
            for (String keyword : rule.getKeywords()) {
                if (!facts.containsKey(keyword) && !containsKeyword(facts, keyword)) {
                    conditionsMet = false;
                    break;
                }
            }

            if (conditionsMet) {
                String inference = "Rule " + rule.getId() + " applied: " + rule.getDescription();
                inferences.add(inference);
                conclusions.put(rule.getAction(), rule.getValue());
            }
        }

        // Forward chaining - apply derived facts
        if (conclusions.containsKey("priority")) {
            String priority = (String) conclusions.get("priority");
            if (knowledgeBase.containsKey("resolution_times")) {
                Map<String, String> resolutionTimes = (Map<String, String>) knowledgeBase.get("resolution_times");
                if (resolutionTimes.containsKey(priority)) {
                    conclusions.put("resolution_time", resolutionTimes.get(priority));
                    inferences.add("Derived resolution_time: " + resolutionTimes.get(priority) + " from priority: " + priority);
                }
            }
        }

        return new InferenceResult(inferences, conclusions, !inferences.isEmpty());
    }

    private boolean containsKeyword(Map<String, Object> facts, String keyword) {
        for (Map.Entry<String, Object> entry : facts.entrySet()) {
            if (entry.getKey().toLowerCase().contains(keyword.toLowerCase())) {
                return true;
            }
            if (entry.getValue() instanceof String) {
                if (((String) entry.getValue()).toLowerCase().contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static class InferenceResult {
        private final List<String> inferences;
        private final Map<String, Object> conclusions;
        private final boolean hasInference;

        public InferenceResult(List<String> inferences, Map<String, Object> conclusions, boolean hasInference) {
            this.inferences = inferences;
            this.conclusions = conclusions;
            this.hasInference = hasInference;
        }

        public List<String> getInferences() { return inferences; }
        public Map<String, Object> getConclusions() { return conclusions; }
        public boolean isHasInference() { return hasInference; }
    }
}