package com.vcsm.service;

import com.vcsm.dto.SentimentResult;
import com.vcsm.model.Sentiment;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class SentimentAnalysisServiceImpl {

    private static final Map<String, Integer> NEGATIVE_KEYWORDS = Map.ofEntries(
        Map.entry("error", 2), Map.entry("problem", 2), Map.entry("broken", 3),
        Map.entry("frustrated", 3), Map.entry("angry", 3), Map.entry("unhappy", 2),
        Map.entry("issue", 1), Map.entry("complaint", 2), Map.entry("urgent", 2),
        Map.entry("terrible", 3), Map.entry("worst", 3), Map.entry("useless", 3),
        Map.entry("waste", 2), Map.entry("fail", 2), Map.entry("crash", 2),
        Map.entry("down", 1), Map.entry("outage", 2), Map.entry("sorry", 1)
    );

    private static final Map<String, Integer> POSITIVE_KEYWORDS = Map.ofEntries(
        Map.entry("thank", 2), Map.entry("great", 2), Map.entry("excellent", 3),
        Map.entry("satisfied", 2), Map.entry("happy", 2), Map.entry("good", 1),
        Map.entry("love", 3), Map.entry("perfect", 3), Map.entry("amazing", 3),
        Map.entry("wonderful", 2), Map.entry("appreciate", 2), Map.entry("solved", 2)
    );

    public SentimentResult analyzeTranscript(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return new SentimentResult(Sentiment.NEUTRAL, 0.0, 0.0, 0, 0, false);
        }

        String normalized = transcript.toLowerCase();
        int negKeywordCount = 0;
        double negativeScore = 0.0;
        int posKeywordCount = 0;
        double positiveScore = 0.0;

        for (Map.Entry<String, Integer> entry : NEGATIVE_KEYWORDS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                negKeywordCount++;
                negativeScore += entry.getValue() * 0.15;
            }
        }

        for (Map.Entry<String, Integer> entry : POSITIVE_KEYWORDS.entrySet()) {
            if (normalized.contains(entry.getKey())) {
                posKeywordCount++;
                positiveScore += entry.getValue() * 0.15;
            }
        }

        negativeScore = Math.min(1.0, negativeScore);
        positiveScore = Math.min(1.0, positiveScore);

        Sentiment sentiment = determineSentiment(negativeScore, positiveScore);
        boolean escalationNeeded = requiresEscalation(sentiment, negativeScore, negKeywordCount);

        return new SentimentResult(sentiment, negativeScore, positiveScore,
                                 negKeywordCount, posKeywordCount, escalationNeeded);
    }

    private Sentiment determineSentiment(double negScore, double posScore) {
        if (negScore > 0.7) return Sentiment.DISTRESSED;
        if (negScore > posScore + 0.2) return Sentiment.NEGATIVE;
        if (posScore > negScore + 0.2) return Sentiment.POSITIVE;
        return Sentiment.NEUTRAL;
    }

    private boolean requiresEscalation(Sentiment sentiment, double negScore, int negKeywordCount) {
        return sentiment == Sentiment.DISTRESSED ||
               negScore > 0.65 ||
               negKeywordCount >= 5;
    }
}
