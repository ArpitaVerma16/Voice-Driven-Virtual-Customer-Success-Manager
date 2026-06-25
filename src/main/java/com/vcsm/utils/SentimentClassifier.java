package com.vcsm.utils;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Component
public class SentimentClassifier {

    private Map<String, Integer> sentimentScores;
    private Map<String, Integer> phraseSentimentScores;

    @PostConstruct
    public void init() {
        // Initialize sentiment scoring
        sentimentScores = new HashMap<>();
        phraseSentimentScores = new HashMap<>();

        // Positive words
        String[] positiveWords = {
                "good", "great", "awesome", "excellent", "happy", "satisfied",
                "thanks", "thank", "love", "perfect", "wonderful", "amazing"
        };

        for (String word : positiveWords) {
            sentimentScores.put(word, 2);
        }

        // Negative words
        String[] negativeWords = {
                "bad", "terrible", "awful", "angry", "unhappy", "frustrated",
                "complaint", "issue", "problem", "poor", "worst", "horrible",
                "disappointed", "annoying", "sick", "tired"
        };

        for (String word : negativeWords) {
            sentimentScores.put(word, -2);
        }

        // Multi-word sentiment phrases
        phraseSentimentScores.put("third time", -2);

        // Strong negative words
        String[] strongNegative = {
                "furious", "enraged", "unacceptable", "useless", "pathetic"
        };

        for (String word : strongNegative) {
            sentimentScores.put(word, -5);
        }
    }

    public SentimentResult analyze(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new SentimentResult("NEUTRAL", 0.5);
        }

        String lowerText = text.toLowerCase();

        // Tokenize and analyze
        String[] tokens = lowerText.split("\\s+");
        int totalScore = 0;
        int wordCount = 0;

        // Check multi-word phrases first
        for (Map.Entry<String, Integer> phrase : phraseSentimentScores.entrySet()) {
            if (lowerText.contains(phrase.getKey())) {
                totalScore += phrase.getValue();
            }
        }

        // Analyze individual words
        for (String token : tokens) {
            // Remove punctuation
            token = token.replaceAll("[^a-zA-Z]", "");

            if (token.isEmpty()) {
                continue;
            }

            if (sentimentScores.containsKey(token)) {
                totalScore += sentimentScores.get(token);
                wordCount++;
            }
        }

        // Existing phrase-based sentiment checks
        if (lowerText.contains("not happy") || lowerText.contains("not satisfied")) {
            totalScore -= 3;
        }

        if (lowerText.contains("very happy") || lowerText.contains("very satisfied")) {
            totalScore += 3;
        }

        // Determine sentiment and confidence
        String sentiment;
        double confidence;

        if (totalScore >= 3) {
            sentiment = "VERY_POSITIVE";
            confidence = Math.min(0.95, 0.6 + (totalScore / 20.0));
        } else if (totalScore >= 1) {
            sentiment = "POSITIVE";
            confidence = 0.6 + (totalScore / 10.0);
        } else if (totalScore <= -5) {
            sentiment = "VERY_NEGATIVE";
            confidence = Math.min(0.95, 0.7 + (Math.abs(totalScore) / 20.0));
        } else if (totalScore <= -1) {
            sentiment = "NEGATIVE";
            confidence = 0.6 + (Math.abs(totalScore) / 10.0);
        } else {
            sentiment = "NEUTRAL";
            confidence = 0.5;
        }

        return new SentimentResult(sentiment, confidence);
    }

    public boolean shouldEscalate(String sentiment) {
        return sentiment.equals("NEGATIVE") || sentiment.equals("VERY_NEGATIVE");
    }

    public static class SentimentResult {
        private final String sentiment;
        private final double confidence;

        public SentimentResult(String sentiment, double confidence) {
            this.sentiment = sentiment;
            this.confidence = confidence;
        }

        public String getSentiment() {
            return sentiment;
        }

        public double getConfidence() {
            return confidence;
        }
    }
}