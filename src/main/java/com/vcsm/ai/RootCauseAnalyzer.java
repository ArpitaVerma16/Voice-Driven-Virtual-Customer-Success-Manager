package com.vcsm.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@lombok.RequiredArgsConstructor
public class RootCauseAnalyzer {

    private final CausalGraphBuilder causalGraphBuilder;

    /**
     * Find root causes for a given issue
     */
    public RootCauseAnalysis findRootCauses(String issue) {
        List<String> directCauses = causalGraphBuilder.getCauses(issue);
        List<String> rootCauses = new ArrayList<>();
        Map<String, Integer> depthMap = new HashMap<>();

        // Find all root causes by traversing the graph
        for (String cause : directCauses) {
            findDeepestCauses(cause, rootCauses, depthMap, 0);
        }

        // Sort by depth (deepest first)
        rootCauses.sort((a, b) -> depthMap.getOrDefault(b, 0) - depthMap.getOrDefault(a, 0));

        // Generate recommendations
        List<String> recommendations = generateRecommendations(rootCauses);

        // Calculate confidence
        double confidence = rootCauses.isEmpty() ? 0.0 : Math.min(0.95, rootCauses.size() * 0.2);

        return new RootCauseAnalysis(
            issue,
            directCauses,
            rootCauses,
            recommendations,
            confidence,
            causalGraphBuilder.getPath(issue, rootCauses.isEmpty() ? issue : rootCauses.get(0))
        );
    }

    private void findDeepestCauses(String node, List<String> rootCauses, Map<String, Integer> depthMap, int depth) {
        List<String> causes = causalGraphBuilder.getCauses(node);
        
        if (causes.isEmpty()) {
            // Leaf node - potential root cause
            if (!rootCauses.contains(node)) {
                rootCauses.add(node);
                depthMap.put(node, depth);
            }
        } else {
            for (String cause : causes) {
                findDeepestCauses(cause, rootCauses, depthMap, depth + 1);
            }
        }
    }

    private List<String> generateRecommendations(List<String> rootCauses) {
        List<String> recommendations = new ArrayList<>();

        for (String cause : rootCauses) {
            switch (cause) {
                case "CONSTRUCTION":
                    recommendations.add("🔧 Schedule construction during day hours only");
                    break;
                case "NEIGHBOR_ACTIVITY":
                    recommendations.add("📢 Issue community guidelines reminder");
                    break;
                case "LACK_SERVICING":
                    recommendations.add("🔧 Implement preventive maintenance schedule");
                    break;
                case "OLD_AGE":
                    recommendations.add("🔄 Plan equipment replacement program");
                    break;
                case "INADEQUATE_SURVEILLANCE":
                    recommendations.add("📹 Upgrade CCTV system and coverage");
                    break;
                case "STAFF_NEGLIGENCE":
                    recommendations.add("📋 Review security protocols and retrain staff");
                    break;
                case "INSUFFICIENT_STAFF":
                    recommendations.add("👥 Hire additional staff during peak hours");
                    break;
                default:
                    recommendations.add("🔍 Investigate " + cause + " further");
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("📊 Need more data to identify root causes");
        }

        return recommendations;
    }

    public static class RootCauseAnalysis {
        private final String issue;
        private final List<String> directCauses;
        private final List<String> rootCauses;
        private final List<String> recommendations;
        private final double confidence;
        private final String causalPath;

        public RootCauseAnalysis(String issue, List<String> directCauses, List<String> rootCauses,
                                 List<String> recommendations, double confidence, String causalPath) {
            this.issue = issue;
            this.directCauses = directCauses;
            this.rootCauses = rootCauses;
            this.recommendations = recommendations;
            this.confidence = confidence;
            this.causalPath = causalPath;
        }

        public String getIssue() { return issue; }
        public List<String> getDirectCauses() { return directCauses; }
        public List<String> getRootCauses() { return rootCauses; }
        public List<String> getRecommendations() { return recommendations; }
        public double getConfidence() { return confidence; }
        public String getCausalPath() { return causalPath; }
    }
}