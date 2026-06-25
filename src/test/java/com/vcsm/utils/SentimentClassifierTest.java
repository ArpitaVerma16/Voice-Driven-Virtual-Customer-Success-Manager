package com.vcsm.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SentimentClassifierTest {

    private SentimentClassifier classifier;

    @BeforeEach
    void setUp() {
        classifier = new SentimentClassifier();
        classifier.init();
    }

    @Test
    void testPositiveSentiment() {
        SentimentClassifier.SentimentResult result = classifier.analyze("This is great!");
        
        assertEquals("POSITIVE", result.getSentiment());
        assertTrue(result.getConfidence() > 0.5);
    }

    @Test
    void testVeryPositiveSentiment() {
        SentimentClassifier.SentimentResult result = classifier.analyze("Excellent! Perfect! Wonderful!");
        
        assertEquals("VERY_POSITIVE", result.getSentiment());
        assertTrue(result.getConfidence() > 0.7);
    }

    @Test
    void testNegativeSentiment() {
        SentimentClassifier.SentimentResult result = classifier.analyze("This is bad and terrible");
        
        assertEquals("NEGATIVE", result.getSentiment());
        assertTrue(result.getConfidence() > 0.5);
    }

    @Test
    void testVeryNegativeSentiment() {
        SentimentClassifier.SentimentResult result = classifier.analyze("This is absolutely unacceptable and useless!");
        
        assertEquals("VERY_NEGATIVE", result.getSentiment());
        assertTrue(result.getConfidence() > 0.6);
    }

    @Test
    void testNeutralSentiment() {
        SentimentClassifier.SentimentResult result = classifier.analyze("The sky is blue");
        
        assertEquals("NEUTRAL", result.getSentiment());
    }

    @Test
    void testEmptyText() {
        SentimentClassifier.SentimentResult result = classifier.analyze("");
        
        assertEquals("NEUTRAL", result.getSentiment());
    }

    @Test
    void testNullText() {
        SentimentClassifier.SentimentResult result = classifier.analyze(null);
        
        assertEquals("NEUTRAL", result.getSentiment());
    }
}