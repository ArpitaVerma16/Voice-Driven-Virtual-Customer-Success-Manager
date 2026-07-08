package com.vcsm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vcsm.model.User;
import com.vcsm.model.Complaint;
import com.vcsm.repository.UserRepository;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.SentimentAnalysisRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

@Profile("dev")
@Service
public class PredictionService {

    private static final Logger log = LoggerFactory.getLogger(PredictionService.class);

    @Value("${ml.service.url:http://localhost:8000}")
    private String mlServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @CircuitBreaker(name = "mlService", fallbackMethod = "predictComplaintsFallback")
    public Map<String, Object> predictComplaints(int days) {
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
        
        try {
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ML service response", e);
        }
    }

    public Map<String, Object> predictComplaintsFallback(int days, Throwable t) {
        log.warn("ML service unavailable for complaint prediction, using fallback: {}", t.getMessage());
        return getFallbackPrediction(days);
    }

    @CircuitBreaker(name = "mlService", fallbackMethod = "predictEventAttendanceFallback")
    public Map<String, Object> predictEventAttendance(Long eventId, List<Map<String, Object>> historicalData) {
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
        
        try {
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ML service response", e);
        }
    }

    public Map<String, Object> predictEventAttendanceFallback(Long eventId, List<Map<String, Object>> historicalData, Throwable t) {
        log.warn("ML service unavailable for event attendance prediction, using fallback: {}", t.getMessage());
        return getFallbackEventPrediction(eventId);
    }

    @CircuitBreaker(name = "mlService", fallbackMethod = "predictSentimentFallback")
    public Map<String, Object> predictSentiment(List<Map<String, Object>> historicalSentiment) {
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
        
        try {
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ML service response", e);
        }
    }

    public Map<String, Object> predictSentimentFallback(List<Map<String, Object>> historicalSentiment, Throwable t) {
        log.warn("ML service unavailable for sentiment prediction, using fallback: {}", t.getMessage());
        return getFallbackSentimentPrediction();
    }

    @CircuitBreaker(name = "mlService", fallbackMethod = "getPeakTimesFallback")
    public Map<String, Object> getPeakTimes() {
        String url = mlServiceUrl + "/api/predict/peak-times";
        
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        try {
            return objectMapper.readValue(response.getBody(), Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse ML service response", e);
        }
    }

    public Map<String, Object> getPeakTimesFallback(Throwable t) {
        log.warn("ML service unavailable for peak times prediction, using fallback: {}", t.getMessage());
        return getFallbackPeakTimes();
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

    public double calculateCustomerDissatisfactionIndex(User user) {
        double score = 0.0;

        // 1. Sentiment Score (up to 40 points)
        List<com.vcsm.model.SentimentAnalysis> sentiments = sentimentAnalysisRepository.findByUser(user);
        long negativeCount = sentiments.stream()
                .filter(s -> "NEGATIVE".equals(s.getSentiment()) || "VERY_NEGATIVE".equals(s.getSentiment()))
                .count();
        if (negativeCount == 1) {
            score += 15.0;
        } else if (negativeCount == 2) {
            score += 30.0;
        } else if (negativeCount >= 3) {
            score += 40.0;
        }

        // 2. Resolution Delay Score (up to 40 points)
        double delayScore = 0.0;
        List<Complaint> complaints = complaintRepository.findByResidentUsernameOrderByCreatedAtDesc(user.getEmail());
        if (!complaints.isEmpty()) {
            long openLongPendingCount = complaints.stream()
                    .filter(c -> c.getStatus() != Complaint.ComplaintStatus.RESOLVED && c.getStatus() != Complaint.ComplaintStatus.CLOSED)
                    .filter(c -> java.time.temporal.ChronoUnit.DAYS.between(c.getCreatedAt(), LocalDateTime.now()) > 3)
                    .count();
            delayScore += Math.min(openLongPendingCount * 15.0, 30.0);

            // Average resolution time for resolved complaints
            List<Complaint> resolved = complaints.stream()
                    .filter(c -> c.getStatus() == Complaint.ComplaintStatus.RESOLVED || c.getStatus() == Complaint.ComplaintStatus.CLOSED)
                    .toList();
            if (!resolved.isEmpty()) {
                double avgResDays = resolved.stream()
                        .mapToDouble(c -> java.time.temporal.ChronoUnit.DAYS.between(c.getCreatedAt(), c.getUpdatedAt()))
                        .average()
                        .orElse(0.0);
                if (avgResDays > 5.0) {
                    delayScore += 10.0;
                } else if (avgResDays > 2.0) {
                    delayScore += 5.0;
                }
            }
        }
        score += Math.min(40.0, delayScore);

        // 3. Interaction & Unresolved Count Score (up to 20 points)
        long unresolvedCount = complaints.stream()
                .filter(c -> c.getStatus() != Complaint.ComplaintStatus.RESOLVED && c.getStatus() != Complaint.ComplaintStatus.CLOSED)
                .count();
        if (unresolvedCount > 3) {
            score += 20.0;
        } else if (unresolvedCount >= 2) {
            score += 10.0;
        }

        return Math.min(100.0, score);
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * SUN") // Sunday at midnight
    public void runWeeklyDissatisfactionAnalysis() {
        log.info("🗓️ Running weekly customer dissatisfaction and churn prediction analysis...");
        List<User> users = userRepository.findAll();
        for (User user : users) {
            double cdi = calculateCustomerDissatisfactionIndex(user);
            user.setDissatisfactionScore(cdi);
            userRepository.save(user);

            if (cdi >= 75.0) {
                log.info("🚨 High dissatisfaction detected for resident: " + user.getEmail() + " (CDI: " + cdi + "). Triggering preemptive outreach.");
                log.info("🚨 High dissatisfaction detected for resident: {} (CDI: {}). Triggering preemptive outreach.", user.getEmail(), cdi);
                try {
                    proactiveOutreachService.sendProactiveOutreach(user.getId(), "email");
                } catch (Exception e) {
                    log.error("❌ Failed to trigger outreach: {}", e.getMessage());
                }
            }
        }
    }

    public void runDissatisfactionAnalysis() {
        runWeeklyDissatisfactionAnalysis();
    }

    public List<User> getHighRiskUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getDissatisfactionScore() >= 75.0)
                .collect(Collectors.toList());
    }
}