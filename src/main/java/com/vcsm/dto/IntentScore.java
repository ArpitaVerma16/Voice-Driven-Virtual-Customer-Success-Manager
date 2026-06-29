package com.vcsm.dto;

import com.vcsm.model.CustomerIntent;

public class IntentScore {
    private final CustomerIntent intent;
    private final double score;
    private final int keywordMatches;

    public IntentScore(CustomerIntent intent, double score, int keywordMatches) {
        this.intent = intent;
        this.score = Math.min(1.0, Math.max(0.0, score));
        this.keywordMatches = keywordMatches;
    }

    public CustomerIntent getIntent() {
        return intent;
    }

    public double getScore() {
        return score;
    }

    public int getKeywordMatches() {
        return keywordMatches;
    }

    @Override
    public String toString() {
        return "IntentScore{" + "intent=" + intent + ", score=" + String.format("%.3f", score) + ", matches=" + keywordMatches + '}';
    }
}
