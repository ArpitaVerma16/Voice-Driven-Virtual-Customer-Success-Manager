package com.vcsm.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CounterfactualSimulator {

    @Autowired
    private CausalGraphBuilder causalGraphBuilder;

    @Autowired
    private RootCauseAnalyzer rootCauseAnalyzer;

    /**
     * Simulate what-if scenarios
     */
    public CounterfactualResult simulate(String issue, String change, String changeValue) {
        // Get current root causes
        RootCauseAnalyzer.RootCauseAnalysis current = rootCauseAnalyzer.findRootCauses(issue);

        // Apply counterfactual change
        List<String> predictedEffects = new ArrayList<>();
        List<String> impactedCauses = new ArrayList<>();

        for (String rootCause : current.getRootCauses()) {
            if (rootCause.equals(change)) {
                impactedCauses.add(rootCause);
                // Find effects of this cause
                List<String> effects = causalGraphBuilder.getEffects(rootCause);
                predictedEffects.addAll(effects);
            }
        }

        // Calculate impact
        double impactScore = predictedEffects.isEmpty() ? 0.0 : Math.min(1.0, predictedEffects.size() * 0.15);

        // Generate what-if analysis
        String analysis = generateAnalysis(issue, change, changeValue, predictedEffects);

        return new CounterfactualResult(
            issue,
            change,
            changeValue,
            impactedCauses,
            predictedEffects,
            impactScore,
            analysis,
            predictedEffects.isEmpty() ? "No significant impact predicted" : "Change may affect " + predictedEffects.size() + " outcomes"
        );
    }

    private String generateAnalysis(String issue, String change, String changeValue, List<String> effects) {
        StringBuffer analysis = new StringBuffer();
        analysis.append("🔍 What-if Analysis for: " + issue + "\n");
        analysis.append("📌 If we change '" + change + "' to '" + changeValue + "':\n\n");

        if (effects.isEmpty()) {
            analysis.append("No significant effects predicted.\n");
        } else {
            analysis.append("Expected effects:\n");
            for (String effect : effects) {
                analysis.append("  • " + effect + " may be impacted\n");
            }
        }

        return analysis.toString();
    }

    public static class CounterfactualResult {
        private final String issue;
        private final String change;
        private final String changeValue;
        private final List<String> impactedCauses;
        private final List<String> predictedEffects;
        private final double impactScore;
        private final String analysis;
        private final String summary;

        public CounterfactualResult(String issue, String change, String changeValue,
                                   List<String> impactedCauses, List<String> predictedEffects,
                                   double impactScore, String analysis, String summary) {
            this.issue = issue;
            this.change = change;
            this.changeValue = changeValue;
            this.impactedCauses = impactedCauses;
            this.predictedEffects = predictedEffects;
            this.impactScore = impactScore;
            this.analysis = analysis;
            this.summary = summary;
        }

        public String getIssue() { return issue; }
        public String getChange() { return change; }
        public String getChangeValue() { return changeValue; }
        public List<String> getImpactedCauses() { return impactedCauses; }
        public List<String> getPredictedEffects() { return predictedEffects; }
        public double getImpactScore() { return impactScore; }
        public String getAnalysis() { return analysis; }
        public String getSummary() { return summary; }
    }
}