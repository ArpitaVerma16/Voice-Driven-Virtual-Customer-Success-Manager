package com.vcsm.service;

import com.vcsm.dto.IntentResult;
import com.vcsm.dto.IntentScore;
import com.vcsm.model.CustomerIntent;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class IntentClassificationService {

    private static final Map<CustomerIntent, Set<String>> INTENT_KEYWORDS = new HashMap<>();
    private static final double CONFIDENCE_THRESHOLD = 0.5;

    static {
        INTENT_KEYWORDS.put(CustomerIntent.BILLING_INQUIRY,
            Set.of("invoice", "bill", "payment", "charge", "subscription", "cost", "price",
                   "refund", "credit", "debit", "statement", "receipt", "billing"));

        INTENT_KEYWORDS.put(CustomerIntent.TECHNICAL_SUPPORT,
            Set.of("error", "crash", "bug", "issue", "problem", "not working", "broken",
                   "fail", "timeout", "latency", "down", "outage", "technical", "debug"));

        INTENT_KEYWORDS.put(CustomerIntent.ACCOUNT_MANAGEMENT,
            Set.of("password", "login", "account", "profile", "settings", "email", "username",
                   "authentication", "reset", "update", "change", "security"));

        INTENT_KEYWORDS.put(CustomerIntent.CANCELLATION_REQUEST,
            Set.of("cancel", "terminate", "delete", "close", "stop", "unsubscribe", "remove",
                   "downgrade", "pause", "discontinue"));

        INTENT_KEYWORDS.put(CustomerIntent.FEATURE_REQUEST,
            Set.of("feature", "request", "suggest", "improvement", "enhancement", "add",
                   "capability", "wish", "like", "would", "could"));
    }

    public IntentResult classify(String transcript) {
        long startTime = System.currentTimeMillis();

        if (transcript == null || transcript.trim().isEmpty()) {
            return new IntentResult(CustomerIntent.GENERAL_INQUIRY, 0.0, Collections.emptyList());
        }

        String normalizedText = transcript.toLowerCase().trim();
        List<IntentScore> scores = new ArrayList<>();

        for (CustomerIntent intent : CustomerIntent.values()) {
            if (intent == CustomerIntent.GENERAL_INQUIRY) {
                continue;
            }
            IntentScore score = computeIntentScore(normalizedText, intent);
            scores.add(score);
        }

        scores.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        IntentScore topScore = scores.isEmpty() ?
            new IntentScore(CustomerIntent.GENERAL_INQUIRY, 0.0, 0) : scores.get(0);

        CustomerIntent finalIntent = topScore.getScore() >= CONFIDENCE_THRESHOLD ?
            topScore.getIntent() : CustomerIntent.GENERAL_INQUIRY;

        long processingTime = System.currentTimeMillis() - startTime;
        if (processingTime > 100) {
            System.err.println("Warning: Intent classification took " + processingTime + "ms");
        }

        return new IntentResult(finalIntent, topScore.getScore(), scores);
    }

    private IntentScore computeIntentScore(String normalizedText, CustomerIntent intent) {
        Set<String> keywords = INTENT_KEYWORDS.get(intent);
        int matches = 0;
        double score = 0.0;

        for (String keyword : keywords) {
            if (normalizedText.contains(keyword)) {
                matches++;
                score += 0.15;
            }
        }

        if (matches > 0) {
            score = Math.min(1.0, score * (1.0 + (matches * 0.1)));
        }

        return new IntentScore(intent, score, matches);
    }
}
