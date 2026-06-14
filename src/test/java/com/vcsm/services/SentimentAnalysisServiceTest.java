package com.vcsm.service;

import com.vcsm.model.SentimentAnalysis;
import com.vcsm.model.User;
import com.vcsm.repository.SentimentAnalysisRepository;
import com.vcsm.utils.SentimentClassifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SentimentAnalysisServiceTest {

    @Mock
    private SentimentClassifier sentimentClassifier;

    @Mock
    private SentimentAnalysisRepository sentimentRepository;

    @InjectMocks
    private SentimentAnalysisService sentimentService;

    private User testUser;
    private SentimentClassifier.SentimentResult positiveResult;
    private SentimentClassifier.SentimentResult negativeResult;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");

        positiveResult = new SentimentClassifier.SentimentResult("POSITIVE", 0.85);
        negativeResult = new SentimentClassifier.SentimentResult("NEGATIVE", 0.75);
    }

    @Test
    void testAnalyzePositiveSentiment() {
        when(sentimentClassifier.analyze("I am very happy with this service"))
            .thenReturn(positiveResult);
        
        SentimentClassifier.SentimentResult result = sentimentClassifier.analyze("I am very happy with this service");
        
        assertNotNull(result);
        assertEquals("POSITIVE", result.getSentiment());
        assertTrue(result.getConfidence() > 0.8);
    }

    @Test
    void testAnalyzeNegativeSentiment() {
        when(sentimentClassifier.analyze("This is terrible service"))
            .thenReturn(negativeResult);
        
        SentimentClassifier.SentimentResult result = sentimentClassifier.analyze("This is terrible service");
        
        assertNotNull(result);
        assertEquals("NEGATIVE", result.getSentiment());
    }

    @Test
    void testShouldEscalateOnNegative() {
        boolean shouldEscalate = sentimentClassifier.shouldEscalate("NEGATIVE");
        assertTrue(shouldEscalate);
    }

    @Test
    void testShouldNotEscalateOnPositive() {
        boolean shouldEscalate = sentimentClassifier.shouldEscalate("POSITIVE");
        assertFalse(shouldEscalate);
    }

    @Test
    void testShouldNotEscalateOnNeutral() {
        boolean shouldEscalate = sentimentClassifier.shouldEscalate("NEUTRAL");
        assertFalse(shouldEscalate);
    }

    @Test
    void testAnalyzeAndProcess_SavesSentiment() {
        String text = "I am happy";
        when(sentimentClassifier.analyze(text)).thenReturn(positiveResult);
        when(sentimentRepository.save(any(SentimentAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SentimentAnalysis result = sentimentService.analyzeAndProcess(testUser.getId(), text);

        assertNotNull(result);
        assertEquals("POSITIVE", result.getSentiment());
        verify(sentimentRepository, times(1)).save(any(SentimentAnalysis.class));
    }
}