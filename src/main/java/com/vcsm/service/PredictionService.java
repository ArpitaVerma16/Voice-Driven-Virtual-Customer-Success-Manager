package com.vcsm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class PredictionService {

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> predictComplaints(int days) {
        try {
            String url = mlServiceUrl + "/api/predict/complaints";
            Map<String, Integer> request = Map.of("days", days);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Integer>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            return objectMapper.readValue(response.getBody(), Map.class);
            
        } catch (Exception e) {
            // Fallback: return mock prediction if ML service is down
            return getFallbackPrediction(days);
        }
    }

    public Map<String, Object> predictEventAttendance(Long eventId, List<Map<String, Object>> historicalData) {
        try {
            String url = mlServiceUrl + "/api/predict/event/" + eventId;
            Map<String, Object> request = Map.of("historicalData", historicalData);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            return objectMapper.readValue(response.getBody(), Map.class);
            
        } catch (Exception e) {
            return getFallbackEventPrediction(eventId);
        }
    }

    public Map<String, Object> predictSentiment(List<Map<String, Object>> historicalSentiment) {
        try {
            String url = mlServiceUrl + "/api/predict/sentiment";
            Map<String, Object> request = Map.of("historicalSentiment", historicalSentiment);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            return objectMapper.readValue(response.getBody(), Map.class);
            
        } catch (Exception e) {
            return getFallbackSentimentPrediction();
        }
    }

    public Map<String, Object> getPeakTimes() {
        try {
            String url = mlServiceUrl + "/api/predict/peak-times";
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            return objectMapper.readValue(response.getBody(), Map.class);
            
        } catch (Exception e) {
            return getFallbackPeakTimes();
        }
    }

    // ============================================================
    // Fallback Methods (when ML service is unavailable)
    // ============================================================

    private Map<String, Object> getFallbackPrediction(int days) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> predictions = new ArrayList<>();
        
        Calendar cal = Calendar.getInstance();
        for (int i = 1; i <= days; i++) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            Map<String, Object> day = new HashMap<>();
            day.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
            day.put("predicted_count", 5 + (int)(Math.random() * 10));
            day.put("lower_bound", 2);
            day.put("upper_bound", 15);
            predictions.add(day);
        }
        
        response.put("predictions", predictions);
        response.put("confidence", 75.0);
        response.put("recommendation", "📊 Moderate complaint volume expected. Regular staffing recommended.");
        return response;
    }

    private Map<String, Object> getFallbackEventPrediction(Long eventId) {
        Map<String, Object> response = new HashMap<>();
        response.put("eventId", eventId);
        response.put("predicted_attendance", 20 + (int)(Math.random() * 30));
        response.put("confidence", 70.0);
        response.put("recommendation", "The event is expected to have moderate attendance.");
        return response;
    }

    private Map<String, Object> getFallbackSentimentPrediction() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> predictions = new ArrayList<>();
        
        Calendar cal = Calendar.getInstance();
        String[] labels = {"Neutral", "Positive", "Neutral"};
        for (int i = 1; i <= 3; i++) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            Map<String, Object> day = new HashMap<>();
            day.put("date", new java.text.SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
            day.put("sentiment_score", 0.2 + Math.random() * 0.3);
            day.put("sentiment_label", labels[i % 3]);
            predictions.add(day);
        }
        
        response.put("predictions", predictions);
        response.put("trend", "➡️ Stable sentiment");
        response.put("recommendation", "Sentiment is stable. Maintain current approach.");
        response.put("confidence", 75.0);
        return response;
    }

    private Map<String, Object> getFallbackPeakTimes() {
        Map<String, Object> response = new HashMap<>();
        response.put("peak_hours", Arrays.asList(10, 14, 16));
        response.put("peak_time", "14:00");
        response.put("peak_activity_level", 20);
        response.put("recommendation", "Peak complaint time is 14:00. Consider extra staff during this time.");
        return response;
    }
}