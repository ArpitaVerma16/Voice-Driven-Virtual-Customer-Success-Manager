package com.vcsm.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class TranscriptSentimentAnalysisService {

    private static final Map<String, Double> SENTIMENT_LEXICON = Map.ofEntries(
        Map.entry("error", -0.9), Map.entry("problem", -0.8), Map.entry("broken", -0.9),
        Map.entry("frustrated", -0.95), Map.entry("angry", -0.95), Map.entry("unhappy", -0.85),
        Map.entry("issue", -0.7), Map.entry("complaint", -0.8), Map.entry("urgent", -0.6),
        Map.entry("help", 0.3), Map.entry("thank", 0.8), Map.entry("great", 0.9),
        Map.entry("excellent", 0.95), Map.entry("satisfied", 0.85), Map.entry("happy", 0.9),
        Map.entry("good", 0.7), Map.entry("please", 0.2), Map.entry("sorry", -0.6)
    );

    public Map<String, Object> analyzeTranscript(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return Map.of("sentiment", "NEUTRAL", "score", 0.0, "escalation_needed", false);
        }

        String normalized = transcript.toLowerCase();
        double totalScore = 0.0;
        int wordMatches = 0;

        for (Map.Entry<String, Double> entry : SENTIMENT_LEXICON.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                totalScore += entry.getValue();
                wordMatches++;
            }
        }

        double avgScore = wordMatches > 0 ? totalScore / wordMatches : 0.0;
        String sentiment = determineSentiment(avgScore);
        boolean escalationNeeded = avgScore < -0.6 || wordMatches > 5;

        return Map.of(
            "sentiment", sentiment,
            "score", Math.round(avgScore * 1000.0) / 1000.0,
            "word_matches", wordMatches,
            "escalation_needed", escalationNeeded
        );
    }

    private String determineSentiment(double score) {
        if (score < -0.5) return "NEGATIVE";
        if (score > 0.5) return "POSITIVE";
        return "NEUTRAL";
    }
}
