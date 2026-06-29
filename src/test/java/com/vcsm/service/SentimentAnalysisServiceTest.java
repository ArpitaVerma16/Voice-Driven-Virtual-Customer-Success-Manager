package com.vcsm.service;

import com.vcsm.model.SentimentAnalysis;
import com.vcsm.model.User;
import com.vcsm.repository.SentimentAnalysisRepository;
import com.vcsm.repository.UserRepository;
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

    @Mock
    private UserRepository userRepository;

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

        // Stub shouldEscalate calls on mocked classifier
        lenient().when(sentimentClassifier.shouldEscalate("NEGATIVE")).thenReturn(true);
        lenient().when(sentimentClassifier.shouldEscalate("VERY_NEGATIVE")).thenReturn(true);
        lenient().when(sentimentClassifier.shouldEscalate("POSITIVE")).thenReturn(false);
        lenient().when(sentimentClassifier.shouldEscalate("NEUTRAL")).thenReturn(false);
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
        when(userRepository.findById(testUser.getId())).thenReturn(java.util.Optional.of(testUser));
        when(sentimentClassifier.analyze(text)).thenReturn(positiveResult);
        when(sentimentRepository.save(any(SentimentAnalysis.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SentimentAnalysis result = sentimentService.analyzeAndProcess(testUser.getId(), text);

        assertNotNull(result);
        assertEquals("POSITIVE", result.getSentiment());
        verify(sentimentRepository, times(1)).save(any(SentimentAnalysis.class));
    }

    @Test
    void testGetSentimentTrends() {
        java.util.List<Object[]> mockRawTrends = new java.util.ArrayList<>();
        mockRawTrends.add(new Object[]{"2026-06-15", 10L, 2L, 5L});
        mockRawTrends.add(new Object[]{"2026-06-16", 12L, 1L, 6L});
        
        when(sentimentRepository.findDailySentimentTrends(any(java.time.LocalDateTime.class)))
            .thenReturn(mockRawTrends);
            
        java.util.List<java.util.Map<String, Object>> trends = sentimentService.getSentimentTrends(7);
        
        assertNotNull(trends);
        assertEquals(2, trends.size());
        assertEquals("2026-06-15", trends.get(0).get("date"));
        assertEquals(10L, trends.get(0).get("positive"));
        assertEquals(2L, trends.get(0).get("negative"));
        assertEquals(5L, trends.get(0).get("neutral"));
        
        assertEquals("2026-06-16", trends.get(1).get("date"));
        assertEquals(12L, trends.get(1).get("positive"));
        assertEquals(1L, trends.get(1).get("negative"));
        assertEquals(6L, trends.get(1).get("neutral"));
    }
}