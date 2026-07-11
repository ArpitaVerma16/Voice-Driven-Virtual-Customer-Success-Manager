package com.vcsm.controller;

import com.vcsm.service.SentimentAnalysisService;
import com.vcsm.utils.SentimentClassifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SentimentControllerTest {

    @Mock
    private SentimentAnalysisService sentimentService;

    @Mock
    private SentimentClassifier sentimentClassifier;

    @InjectMocks
    private SentimentController sentimentController;

    @Test
    void testGetSentimentTrends() {
        List<Map<String, Object>> mockTrends = new ArrayList<>();
        Map<String, Object> day1 = new HashMap<>();
        day1.put("date", "2026-06-15");
        day1.put("positive", 10L);
        day1.put("negative", 2L);
        day1.put("neutral", 5L);
        mockTrends.add(day1);

        when(sentimentService.getSentimentTrends(7)).thenReturn(mockTrends);

        ResponseEntity<List<Map<String, Object>>> response = sentimentController.getSentimentTrends(7);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        List<Map<String, Object>> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertEquals("2026-06-15", body.get(0).get("date"));
        assertEquals(10L, body.get(0).get("positive"));
    }
}
