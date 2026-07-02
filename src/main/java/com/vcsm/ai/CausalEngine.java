package com.vcsm.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@lombok.RequiredArgsConstructor
public class CausalEngine {

    private final RootCauseAnalyzer rootCauseAnalyzer;

    private final CounterfactualSimulator counterfactualSimulator;

    private final CausalGraphBuilder causalGraphBuilder;

    /**
     * Complete causal analysis
     */
    public CausalAnalysis analyze(String issue) {
        // Root cause analysis
        RootCauseAnalyzer.RootCauseAnalysis rootCause = rootCauseAnalyzer.findRootCauses(issue);

        // Generate potential interventions
        List<String> interventions = generateInterventions(rootCause.getRootCauses());

        // Calculate overall confidence
        double overallConfidence = rootCause.getConfidence();

        return new CausalAnalysis(
            issue,
            rootCause,
            interventions,
            overallConfidence,
            generateImpactSummary(rootCause)
        );
    }

    private List<String> generateInterventions(List<String> rootCauses) {
        List<String> interventions = new ArrayList<>();
        for (String cause : rootCauses) {
            switch (cause) {
                case "CONSTRUCTION":
                    interventions.add("🔧 Implement noise barriers");
                    break;
                case "NEIGHBOR_ACTIVITY":
                    interventions.add("📢 Community awareness program");
                    break;
                case "LACK_SERVICING":
                    interventions.add("🔧 Preventive maintenance program");
                    break;
                case "OLD_AGE":
                    interventions.add("🔄 Equipment replacement plan");
                    break;
                case "INADEQUATE_SURVEILLANCE":
                    interventions.add("📹 Security upgrade");
                    break;
                case "STAFF_NEGLIGENCE":
                    interventions.add("📋 Staff training program");
                    break;
                default:
                    interventions.add("🔍 Further investigation needed for " + cause);
            }
        }
        return interventions;
    }

    private String generateImpactSummary(RootCauseAnalyzer.RootCauseAnalysis analysis) {
        if (analysis.getRootCauses().isEmpty()) {
            return "No clear root causes identified";
        }
        return "Root causes: " + String.join(", ", analysis.getRootCauses());
    }

    public static class CausalAnalysis {
        private final String issue;
        private final RootCauseAnalyzer.RootCauseAnalysis rootCause;
        private final List<String> interventions;
        private final double confidence;
        private final String impactSummary;

        public CausalAnalysis(String issue, RootCauseAnalyzer.RootCauseAnalysis rootCause,
                             List<String> interventions, double confidence, String impactSummary) {
            this.issue = issue;
            this.rootCause = rootCause;
            this.interventions = interventions;
            this.confidence = confidence;
            this.impactSummary = impactSummary;
        }

        public String getIssue() { return issue; }
        public RootCauseAnalyzer.RootCauseAnalysis getRootCause() { return rootCause; }
        public List<String> getInterventions() { return interventions; }
        public double getConfidence() { return confidence; }
        public String getImpactSummary() { return impactSummary; }
    }
}