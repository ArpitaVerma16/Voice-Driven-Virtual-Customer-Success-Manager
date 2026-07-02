package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.Decision;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.DecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DecisionEngine {

    @Autowired
    private DecisionRepository decisionRepository;

    @Autowired
    private ExplainabilityService explainabilityService;

    @Autowired
    private ReinforcementLearningService rlService;

    @Autowired
    private ComplaintRepository complaintRepository;

    /**
     * Make autonomous decision for a complaint
     */
    public Decision makeDecision(Complaint complaint) {
        // Gather factors
        Map<String, Object> factors = gatherFactors(complaint);

        // Determine decision type
        String decisionType = determineDecisionType(factors);
        String action = rlService.getBestAction(decisionType, getAvailableActions(decisionType));

        // Create decision
        Decision decision = new Decision();
        decision.setDecisionType(decisionType);
        decision.setEntityId(complaint.getId());
        decision.setDecisionReason(generateReason(decisionType, factors));
        decision.setConfidenceScore(calculateConfidence(factors));
        decision.setExecutedBy("SYSTEM");
        decision.setOutcome("PENDING");

        // Generate explanation
        ExplainabilityService.DecisionExplanation explanation = 
            explainabilityService.explain(decisionType, factors);
        decision.setExplanation(explainabilityService.getSummary(explanation));

        // Execute decision
        executeDecision(decision, complaint);

        return decisionRepository.save(decision);
    }

    private Map<String, Object> gatherFactors(Complaint complaint) {
        Map<String, Object> factors = new HashMap<>();
        factors.put("id", complaint.getId());
        factors.put("category", complaint.getCategory());
        factors.put("status", complaint.getStatus());
        factors.put("createdAt", complaint.getCreatedAt());
        factors.put("urgency", calculateUrgency(complaint));
        factors.put("recurrence", countRecurrences(complaint));
        factors.put("frustrated", detectFrustration(complaint));
        
        return factors;
    }

    private int calculateUrgency(Complaint complaint) {
        int urgency = 50;
        if (complaint.getCreatedAt() != null) {
            long hours = ChronoUnit.HOURS.between(complaint.getCreatedAt(), LocalDateTime.now());
            if (hours > 48) urgency += 20;
            else if (hours > 24) urgency += 10;
        }
        if (complaint.getPriority() != null) {
            if ("HIGH".equals(complaint.getPriority())) urgency += 20;
            if ("CRITICAL".equals(complaint.getPriority())) urgency += 30;
        }
        return Math.min(100, urgency);
    }

    private int countRecurrences(Complaint complaint) {
        if (complaint.getResidentUsername() == null) return 0;
        List<Complaint> userComplaints = complaintRepository
            .findByResidentUsernameOrderByCreatedAtDesc(complaint.getResidentUsername());
        return (int) userComplaints.stream()
            .filter(c -> c.getCategory() == complaint.getCategory())
            .count();
    }

    private boolean detectFrustration(Complaint complaint) {
        String desc = complaint.getDescription().toLowerCase();
        return desc.contains("again") || desc.contains("third time") || 
               desc.contains("frustrated") || desc.contains("still not");
    }

    private String determineDecisionType(Map<String, Object> factors) {
        int urgency = (int) factors.getOrDefault("urgency", 0);
        int recurrence = (int) factors.getOrDefault("recurrence", 0);
        boolean frustrated = (boolean) factors.getOrDefault("frustrated", false);

        if (urgency > 70 && frustrated) return "ESCALATE";
        if (urgency > 60) return "PRIORITIZE";
        if (recurrence > 3) return "ASSIGN";
        return "RESOLVE";
    }

    private List<String> getAvailableActions(String decisionType) {
        switch (decisionType) {
            case "PRIORITIZE":
                return Arrays.asList("HIGH", "MEDIUM", "LOW");
            case "ESCALATE":
                return Arrays.asList("SUPERVISOR", "MANAGER", "SENIOR");
            case "ASSIGN":
                return Arrays.asList("EXPERT", "GENERAL", "AUTO");
            default:
                return Arrays.asList("AUTO", "MANUAL");
        }
    }

    private String generateReason(String decisionType, Map<String, Object> factors) {
        switch (decisionType) {
            case "PRIORITIZE":
                return "Urgency: " + factors.get("urgency") + " | Category: " + factors.get("category");
            case "ESCALATE":
                return "Frustration detected | Urgency: " + factors.get("urgency");
            case "ASSIGN":
                return "Recurrence: " + factors.get("recurrence") + " times";
            default:
                return "Standard handling";
        }
    }

    private double calculateConfidence(Map<String, Object> factors) {
        double confidence = 0.7;
        if ((boolean) factors.getOrDefault("frustrated", false)) confidence += 0.2;
        if ((int) factors.getOrDefault("urgency", 0) > 70) confidence += 0.1;
        return Math.min(0.95, confidence);
    }

    private void executeDecision(Decision decision, Complaint complaint) {
        String type = decision.getDecisionType();
        switch (type) {
            case "PRIORITIZE":
                complaint.setPriority("HIGH");
                complaintRepository.save(complaint);
                break;
            case "ESCALATE":
                complaint.setPriority("CRITICAL");
                complaintRepository.save(complaint);
                break;
            case "ASSIGN":
                // Auto-assign logic
                break;
            default:
                // No action
                break;
        }
    }
}