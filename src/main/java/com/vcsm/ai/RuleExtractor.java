package com.vcsm.ai;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class RuleExtractor {

    private final List<Rule> ruleSet = new ArrayList<>();

    public RuleExtractor() {
        initializeRules();
    }

    private void initializeRules() {
        // Complaint rules
        ruleSet.add(new Rule(
            "COMPLAINT_RULE_1",
            "If complaint contains 'noise' AND 'night' THEN priority = HIGH",
            "COMPLAINT",
            Arrays.asList("noise", "night"),
            "priority",
            "HIGH"
        ));

        ruleSet.add(new Rule(
            "COMPLAINT_RULE_2",
            "If complaint contains 'security' OR 'break-in' THEN priority = CRITICAL",
            "COMPLAINT",
            Arrays.asList("security", "break-in"),
            "priority",
            "CRITICAL"
        ));

        ruleSet.add(new Rule(
            "COMPLAINT_RULE_3",
            "If complaint contains 'water' AND ('leak' OR 'flood') THEN category = MAINTENANCE",
            "COMPLAINT",
            Arrays.asList("water", "leak", "flood"),
            "category",
            "MAINTENANCE"
        ));

        ruleSet.add(new Rule(
            "COMPLAINT_RULE_4",
            "If complaint contains 'parking' AND 'blocked' THEN category = PARKING",
            "COMPLAINT",
            Arrays.asList("parking", "blocked"),
            "category",
            "PARKING"
        ));

        // Escalation rules
        ruleSet.add(new Rule(
            "ESCALATION_RULE_1",
            "If priority = CRITICAL AND unresolved > 2 THEN escalate = TRUE",
            "ESCALATION",
            Arrays.asList("CRITICAL"),
            "escalate",
            "TRUE"
        ));

        // Resolution rules
        ruleSet.add(new Rule(
            "RESOLUTION_RULE_1",
            "If category = NOISE AND priority = LOW THEN resolution_time = 24h",
            "RESOLUTION",
            Arrays.asList("NOISE", "LOW"),
            "resolution_time",
            "24h"
        ));

        ruleSet.add(new Rule(
            "RESOLUTION_RULE_2",
            "If category = MAINTENANCE AND priority = HIGH THEN resolution_time = 4h",
            "RESOLUTION",
            Arrays.asList("MAINTENANCE", "HIGH"),
            "resolution_time",
            "4h"
        ));
    }

    public List<Rule> extractRules(String context) {
        List<Rule> matchedRules = new ArrayList<>();
        String lowerContext = context.toLowerCase();

        for (Rule rule : ruleSet) {
            boolean matches = true;
            for (String keyword : rule.getKeywords()) {
                if (!lowerContext.contains(keyword.toLowerCase())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                matchedRules.add(rule);
            }
        }

        return matchedRules;
    }

    public List<Rule> getRulesByDomain(String domain) {
        List<Rule> result = new ArrayList<>();
        for (Rule rule : ruleSet) {
            if (rule.getDomain().equals(domain)) {
                result.add(rule);
            }
        }
        return result;
    }

    public void addRule(Rule rule) {
        ruleSet.add(rule);
    }

    public static class Rule {
        private final String id;
        private final String description;
        private final String domain;
        private final List<String> keywords;
        private final String action;
        private final String value;

        public Rule(String id, String description, String domain, List<String> keywords, String action, String value) {
            this.id = id;
            this.description = description;
            this.domain = domain;
            this.keywords = keywords;
            this.action = action;
            this.value = value;
        }

        public String getId() { return id; }
        public String getDescription() { return description; }
        public String getDomain() { return domain; }
        public List<String> getKeywords() { return keywords; }
        public String getAction() { return action; }
        public String getValue() { return value; }
    }
}