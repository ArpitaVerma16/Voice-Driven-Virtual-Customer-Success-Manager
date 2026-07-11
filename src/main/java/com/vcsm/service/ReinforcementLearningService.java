package com.vcsm.service;

import com.vcsm.model.Decision;
import com.vcsm.repository.DecisionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@lombok.RequiredArgsConstructor
public class ReinforcementLearningService {

    private final DecisionRepository decisionRepository;

    // Q-learning table
    private final Map<String, Map<String, Double>> qTable = new HashMap<>();

    /**
     * Get best action for state
     */
    public String getBestAction(String state, List<String> actions) {
        Map<String, Double> stateActions = qTable.computeIfAbsent(state, k -> new HashMap<>());
        
        // Initialize if not exists
        for (String action : actions) {
            stateActions.putIfAbsent(action, 0.0);
        }

        // Find best action
        return stateActions.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(actions.isEmpty() ? null : actions.get(0));
    }

    /**
     * Update Q-value based on reward
     */
    public void updateQValue(String state, String action, double reward) {
        Map<String, Double> stateActions = qTable.computeIfAbsent(state, k -> new HashMap<>());
        double currentQ = stateActions.getOrDefault(action, 0.0);
        double learningRate = 0.1;
        double discountFactor = 0.9;
        
        // Simple Q-learning update
        double newQ = currentQ + learningRate * (reward + discountFactor * getMaxQ(state) - currentQ);
        stateActions.put(action, Math.max(0, newQ));
    }

    private double getMaxQ(String state) {
        Map<String, Double> stateActions = qTable.get(state);
        if (stateActions == null || stateActions.isEmpty()) return 0;
        return stateActions.values().stream().max(Double::compareTo).orElse(0.0);
    }

    /**
     * Learn from decision outcomes
     */
    public void learnFromOutcome(Decision decision) {
        String state = decision.getDecisionType() + ":" + decision.getEntityId();
        String action = decision.getOutcome();
        double reward = calculateReward(decision);
        
        updateQValue(state, action, reward);
    }

    private double calculateReward(Decision decision) {
        if ("SUCCESS".equals(decision.getOutcome())) {
            return 10.0;
        } else if ("FAILURE".equals(decision.getOutcome())) {
            return -5.0;
        }
        return 0.0;
    }

    /**
     * Get learning statistics
     */
    public Map<String, Object> getLearningStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalStates", qTable.size());
        stats.put("totalActions", qTable.values().stream().mapToInt(Map::size).sum());
        stats.put("learnedDecisions", decisionRepository.count());
        return stats;
    }
}