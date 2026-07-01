package com.vcsm.service;

import com.vcsm.repository.SentimentAnalysisRepository;
import com.vcsm.repository.VoiceCommandRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalyticsService {

    private final SentimentAnalysisRepository sentimentRepository;
    private final VoiceCommandRepository voiceCommandRepository;

    @Autowired
    public AnalyticsService(SentimentAnalysisRepository sentimentRepository, VoiceCommandRepository voiceCommandRepository) {
        this.sentimentRepository = sentimentRepository;
        this.voiceCommandRepository = voiceCommandRepository;
    }

    public Map<String, Object> getDailySentiment(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> results = sentimentRepository.findDailySentimentTrends(startDate);
        
        Map<String, Object> dailyData = new LinkedHashMap<>();
        for (Object[] row : results) {
            String date = row[0].toString();
            long pos = ((Number) row[1]).longValue();
            long neg = ((Number) row[2]).longValue();
            long neu = ((Number) row[3]).longValue();
            
            long total = pos + neg + neu;
            double avgScore = total == 0 ? 0 : (pos * 1.0 - neg * 1.0) / total;
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("positive", pos);
            metrics.put("negative", neg);
            metrics.put("neutral", neu);
            metrics.put("averageScore", avgScore);
            
            dailyData.put(date, metrics);
        }
        return dailyData;
    }

    public Map<String, Long> getTopIntents(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        List<Object[]> results = voiceCommandRepository.findTopIntents(startDate);
        
        Map<String, Long> topIntents = new LinkedHashMap<>();
        for (Object[] row : results) {
            String intent = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            if (intent != null && !intent.isEmpty()) {
                topIntents.put(intent, count);
            }
        }
        return topIntents;
    }

    public Map<String, Object> getEscalationRate() {
        long total = sentimentRepository.count();
        long escalated = sentimentRepository.countByWasEscalated(true);
        double rate = total == 0 ? 0.0 : ((double) escalated / total) * 100.0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalSessionsAnalysed", total);
        result.put("escalatedSessions", escalated);
        result.put("escalationRatePercentage", rate);
        return result;
    }
}
