package com.vcsm.security;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class AdversarialDefense {

    /**
     * Detect adversarial input
     */
    public DefenseResult detectAdversarial(String input, String modelType) {
        List<AttackIndicator> indicators = new ArrayList<>();
        double overallRisk = 0.0;

        // Check for common attack patterns
        if (containsSuspiciousPatterns(input)) {
            indicators.add(new AttackIndicator("SUSPICIOUS_PATTERN", 0.7, "Contains suspicious character patterns"));
            overallRisk += 0.3;
        }

        // Check for unusual length
        if (isUnusualLength(input)) {
            indicators.add(new AttackIndicator("UNUSUAL_LENGTH", 0.5, "Input length is unusual for this context"));
            overallRisk += 0.2;
        }

        // Check for repeated patterns
        if (hasRepeatedPatterns(input)) {
            indicators.add(new AttackIndicator("REPEATED_PATTERNS", 0.6, "Repeated patterns detected"));
            overallRisk += 0.25;
        }

        // Check for special characters
        if (hasExcessiveSpecialChars(input)) {
            indicators.add(new AttackIndicator("EXCESSIVE_SPECIAL_CHARS", 0.4, "Excessive special characters"));
            overallRisk += 0.15;
        }

        // Normalize risk
        overallRisk = Math.min(1.0, overallRisk);

        // Determine if adversarial
        boolean isAdversarial = overallRisk > 0.5;

        // Classify attack type
        String attackType = classifyAttack(indicators);

        return new DefenseResult(
            isAdversarial,
            overallRisk,
            indicators,
            attackType,
            isAdversarial ? "BLOCKED" : "ALLOWED",
            generateRecommendation(overallRisk)
        );
    }

    private boolean containsSuspiciousPatterns(String input) {
        String lower = input.toLowerCase();
        return lower.contains("<script>") || 
               lower.contains("javascript:") ||
               lower.contains("eval(") ||
               lower.contains("alert(") ||
               lower.contains("onclick=");
    }

    private boolean isUnusualLength(String input) {
        return input.length() > 1000 || input.length() < 3;
    }

    private boolean hasRepeatedPatterns(String input) {
        // Check for repeated characters/words
        return input.matches(".*(.)\\1{10,}.*");
    }

    private boolean hasExcessiveSpecialChars(String input) {
        long specialCount = input.chars()
            .filter(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))
            .count();
        return specialCount > input.length() * 0.3;
    }

    private String classifyAttack(List<AttackIndicator> indicators) {
        if (indicators.isEmpty()) return org.springframework.http.ResponseEntity.ok("NONE");
        
        for (AttackIndicator ind : indicators) {
            if (ind.getType().equals("SUSPICIOUS_PATTERN")) {
                return org.springframework.http.ResponseEntity.ok("INJECTION");
            }
            if (ind.getType().equals("UNUSUAL_LENGTH")) {
                return org.springframework.http.ResponseEntity.ok("DOS");
            }
            if (ind.getType().equals("REPEATED_PATTERNS")) {
                return org.springframework.http.ResponseEntity.ok("REPLAY");
            }
        }
        return org.springframework.http.ResponseEntity.ok("UNKNOWN");
    }

    private String generateRecommendation(double risk) {
        if (risk > 0.8) {
            return org.springframework.http.ResponseEntity.ok("🚨 HIGH RISK: Block input immediately and log incident");
        } else if (risk > 0.5) {
            return org.springframework.http.ResponseEntity.ok("⚠️ MEDIUM RISK: Sanitize input and flag for review");
        } else if (risk > 0.2) {
            return org.springframework.http.ResponseEntity.ok("ℹ️ LOW RISK: Allow input but monitor");
        }
        return org.springframework.http.ResponseEntity.ok("✅ SAFE: No action needed");
    }

    public static class AttackIndicator {
        private final String type;
        private final double confidence;
        private final String description;

        public AttackIndicator(String type, double confidence, String description) {
            this.type = type;
            this.confidence = confidence;
            this.description = description;
        }

        public String getType() { return type; }
        public double getConfidence() { return confidence; }
        public String getDescription() { return description; }
    }

    public static class DefenseResult {
        private final boolean isAdversarial;
        private final double riskScore;
        private final List<AttackIndicator> indicators;
        private final String attackType;
        private final String action;
        private final String recommendation;

        public DefenseResult(boolean isAdversarial, double riskScore, List<AttackIndicator> indicators,
                            String attackType, String action, String recommendation) {
            this.isAdversarial = isAdversarial;
            this.riskScore = riskScore;
            this.indicators = indicators;
            this.attackType = attackType;
            this.action = action;
            this.recommendation = recommendation;
        }

        public boolean isAdversarial() { return isAdversarial; }
        public double getRiskScore() { return riskScore; }
        public List<AttackIndicator> getIndicators() { return indicators; }
        public String getAttackType() { return attackType; }
        public String getAction() { return action; }
        public String getRecommendation() { return recommendation; }
    }
}