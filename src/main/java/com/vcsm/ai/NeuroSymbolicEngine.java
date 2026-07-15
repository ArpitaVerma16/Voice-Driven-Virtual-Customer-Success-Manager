package com.vcsm.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@lombok.RequiredArgsConstructor
public class NeuroSymbolicEngine {

    private final RuleExtractor ruleExtractor;

    private final InferenceEngine inferenceEngine;

    /**
     * Process input using neuro-symbolic approach
     */
    public NeuroSymbolicResult process(String input, String domain) {
        // Step 1: Neural perception (extract features)
        Map<String, Object> features = extractFeatures(input);

        // Step 2: Symbolic reasoning (apply rules)
        List<RuleExtractor.Rule> rules = ruleExtractor.extractRules(input);
        InferenceEngine.InferenceResult inferenceResult = inferenceEngine.reason(features, rules);

        // Step 3: Generate explanations
        List<String> explanations = generateExplanations(input, features, inferenceResult);

        // Step 4: Generate recommendations
        List<String> recommendations = generateRecommendations(inferenceResult, domain);

        return new NeuroSymbolicResult(
            input,
            features,
            inferenceResult,
            explanations,
            recommendations,
            inferenceResult.isHasInference()
        );
    }

    private Map<String, Object> extractFeatures(String input) {
        Map<String, Object> features = new HashMap<>();
        String lower = input.toLowerCase();

        // Extract keywords
        if (lower.contains("noise")) features.put("noise", true);
        if (lower.contains("night")) features.put("night", true);
        if (lower.contains("security")) features.put("security", true);
        if (lower.contains("break-in")) features.put("break-in", true);
        if (lower.contains("water")) features.put("water", true);
        if (lower.contains("leak")) features.put("leak", true);
        if (lower.contains("flood")) features.put("flood", true);
        if (lower.contains("parking")) features.put("parking", true);
        if (lower.contains("blocked")) features.put("blocked", true);

        // Extract context
        features.put("input_length", input.length());
        features.put("word_count", input.split("\\s+").length);

        return features;
    }

    private List<String> generateExplanations(String input, Map<String, Object> features, 
                                             InferenceEngine.InferenceResult inference) {
        List<String> explanations = new ArrayList<>();
        explanations.add("🔍 Input analyzed: " + input);
        
        if (!features.isEmpty()) {
            explanations.add("📊 Extracted features: " + features.keySet());
        }

        if (inference.isHasInference()) {
            explanations.add("🧠 Reasoning applied:");
            for (String inferenceStr : inference.getInferences()) {
                explanations.add("  • " + inferenceStr);
            }
        } else {
            explanations.add("ℹ️ No rules applied. Using default processing.");
        }

        return explanations;
    }

    private List<String> generateRecommendations(InferenceEngine.InferenceResult inference, String domain) {
        List<String> recommendations = new ArrayList<>();

        if (inference.isHasInference()) {
            Map<String, Object> conclusions = inference.getConclusions();
            
            if (conclusions.containsKey("priority")) {
                String priority = (String) conclusions.get("priority");
                recommendations.add("🎯 Priority assigned: " + priority);
                
                if ("CRITICAL".equals(priority)) {
                    recommendations.add("🚨 Immediate action required!");
                } else if ("HIGH".equals(priority)) {
                    recommendations.add("⏰ Respond within 4 hours.");
                }
            }

            if (conclusions.containsKey("category")) {
                recommendations.add("📂 Category: " + conclusions.get("category"));
            }

            if (conclusions.containsKey("resolution_time")) {
                recommendations.add("⏳ Expected resolution time: " + conclusions.get("resolution_time"));
            }
        } else {
            recommendations.add("📌 No specific recommendations. General processing.");
            recommendations.add("💡 Please provide more details for better reasoning.");
        }

        return recommendations;
    }

    public static class NeuroSymbolicResult {
        private final String input;
        private final Map<String, Object> features;
        private final InferenceEngine.InferenceResult inferenceResult;
        private final List<String> explanations;
        private final List<String> recommendations;
        private final boolean success;

        public NeuroSymbolicResult(String input, Map<String, Object> features,
                                   InferenceEngine.InferenceResult inferenceResult,
                                   List<String> explanations, List<String> recommendations,
                                   boolean success) {
            this.input = input;
            this.features = features;
            this.inferenceResult = inferenceResult;
            this.explanations = explanations;
            this.recommendations = recommendations;
            this.success = success;
        }

        public String getInput() { return input; }
        public Map<String, Object> getFeatures() { return features; }
        public InferenceEngine.InferenceResult getInferenceResult() { return inferenceResult; }
        public List<String> getExplanations() { return explanations; }
        public List<String> getRecommendations() { return recommendations; }
        public boolean isSuccess() { return success; }
    }
}