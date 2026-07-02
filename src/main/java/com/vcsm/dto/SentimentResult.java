package com.vcsm.dto;

import com.vcsm.model.Sentiment;

public class SentimentResult {
    private final Sentiment sentiment;
    private final double negativeScore;
    private final double positiveScore;
    private final int negativeKeywords;
    private final int positiveKeywords;
    private final boolean requiresEscalation;

    public SentimentResult(Sentiment sentiment, double negativeScore, double positiveScore,
                          int negKeywords, int posKeywords, boolean requiresEscalation) {
        this.sentiment = sentiment;
        this.negativeScore = negativeScore;
        this.positiveScore = positiveScore;
        this.negativeKeywords = negKeywords;
        this.positiveKeywords = posKeywords;
        this.requiresEscalation = requiresEscalation;
    }

    public Sentiment getSentiment() { return sentiment; }
    public double getNegativeScore() { return negativeScore; }
    public double getPositiveScore() { return positiveScore; }
    public int getNegativeKeywords() { return negativeKeywords; }
    public int getPositiveKeywords() { return positiveKeywords; }
    public boolean isRequiresEscalation() { return requiresEscalation; }

    public String getEscalationReason() {
        if (!requiresEscalation) return null;
        if (sentiment == Sentiment.DISTRESSED) return org.springframework.http.ResponseEntity.ok("Distressed customer detected");
        if (negativeScore > 0.7) return org.springframework.http.ResponseEntity.ok("High negativity score");
        if (negativeKeywords > 5) return org.springframework.http.ResponseEntity.ok("Multiple escalation keywords");
        return org.springframework.http.ResponseEntity.ok("Escalation threshold exceeded");
    }
}
