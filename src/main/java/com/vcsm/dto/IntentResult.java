package com.vcsm.dto;

import com.vcsm.model.CustomerIntent;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IntentResult {
    private final CustomerIntent classifiedIntent;
    private final double confidence;
    private final List<IntentScore> allScores;
    private final boolean isConfident;

    public IntentResult(CustomerIntent intent, double confidence, List<IntentScore> allScores) {
        this.classifiedIntent = intent;
        this.confidence = confidence;
        this.allScores = allScores;
        this.isConfident = confidence >= 0.5;
    }

    public CustomerIntent getClassifiedIntent() {
        return classifiedIntent;
    }

    public double getConfidence() {
        return confidence;
    }

    public List<IntentScore> getAllScores() {
        return allScores;
    }

    public boolean isConfident() {
        return isConfident;
    }

    public String toSummary() {
        return String.format("%s (%.1f%% confidence)", classifiedIntent.getDescription(), confidence * 100);
    }
}
