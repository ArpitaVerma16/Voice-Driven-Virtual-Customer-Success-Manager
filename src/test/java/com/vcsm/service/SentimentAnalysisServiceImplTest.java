package com.vcsm.service;

import com.vcsm.dto.SentimentResult;
import com.vcsm.model.Sentiment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SentimentAnalysisServiceImplTest {

    private SentimentAnalysisServiceImpl service;

    @BeforeEach
    public void setUp() {
        service = new SentimentAnalysisServiceImpl();
    }

    @Test
    public void testPositiveSentiment() {
        SentimentResult result = service.analyzeTranscript("Thank you so much! Great service and excellent support");
        assertEquals(Sentiment.POSITIVE, result.getSentiment());
        assertFalse(result.isRequiresEscalation());
    }

    @Test
    public void testNegativeSentiment() {
        SentimentResult result = service.analyzeTranscript("This is broken and not working properly");
        assertEquals(Sentiment.NEGATIVE, result.getSentiment());
    }

    @Test
    public void testDistressedSentiment() {
        SentimentResult result = service.analyzeTranscript("This is the worst system ever! I'm angry and frustrated with this broken service!");
        assertEquals(Sentiment.DISTRESSED, result.getSentiment());
        assertTrue(result.isRequiresEscalation());
    }

    @Test
    public void testEscalationThreshold() {
        SentimentResult result = service.analyzeTranscript("problem error broken issue complaint frustrated angry");
        assertTrue(result.isRequiresEscalation());
    }

    @Test
    public void testEmptyTranscriptHandling() {
        SentimentResult result = service.analyzeTranscript("");
        assertEquals(Sentiment.NEUTRAL, result.getSentiment());
    }
}
