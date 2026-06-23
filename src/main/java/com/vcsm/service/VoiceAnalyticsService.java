package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.model.VoiceAnalytics;
import com.vcsm.repository.VoiceAnalyticsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


import java.time.format.DateTimeFormatter;

import java.util.*;

@Service
public class VoiceAnalyticsService {
    
    @Autowired
    private VoiceAnalyticsRepository voiceAnalyticsRepository;
    
    public void logCommand(User user, String commandText, String intent, boolean success, long responseTime) {
        VoiceAnalytics analytics = new VoiceAnalytics(user, commandText, intent, success, responseTime);
        voiceAnalyticsRepository.save(analytics);
    }
    

    public Map<String, Object> getSummary() {
        Map<String, Object> stats = new LinkedHashMap<>();

        long totalCommands = voiceAnalyticsRepository.count();
        stats.put("totalCommands", totalCommands);

        long uniqueUsers = voiceAnalyticsRepository.getUniqueUsersCount();
        stats.put("uniqueUsers", uniqueUsers);

        return stats;
    }

    public Map<String, Object> getAnalytics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        // Total commands
        long totalCommands = voiceAnalyticsRepository.count();
        stats.put("totalCommands", totalCommands);
        
        // Unique users
        long uniqueUsers = voiceAnalyticsRepository.getUniqueUsersCount();
        stats.put("uniqueUsers", uniqueUsers);
        
        // Success rate

        List<Object[]> successData = voiceAnalyticsRepository.countBySuccess();
        long successCount = 0;
        long failCount = 0;
        for (Object[] data : successData) {
            boolean isSuccess = (boolean) data[0];
            long count = (long) data[1];
            if (isSuccess) successCount = count;
            else failCount = count;
        }
        double successRate = totalCommands > 0 ? (successCount * 100.0 / totalCommands) : 0;
        stats.put("successRate", Math.round(successRate));
        

        Double avgResponseTime = voiceAnalyticsRepository.getAverageResponseTime();
        stats.put("averageResponseTime", avgResponseTime != null ? Math.round(avgResponseTime) : 0);
        
        // Recent commands (last 7 days)

        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recentCommands = voiceAnalyticsRepository.countRecentCommands(sevenDaysAgo);
        stats.put("recentCommands", recentCommands);
        
        return stats;
    }
    
    public List<Map<String, Object>> getCommandStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Object[]> data = voiceAnalyticsRepository.countByIntent();
        
        for (Object[] row : data) {
            Map<String, Object> item = new HashMap<>();
            item.put("intent", row[0] != null ? row[0] : "unknown");
            item.put("count", row[1]);
            result.add(item);
        }
        return result;
    }
    
    public List<Map<String, Object>> getHourlyStats() {
        List<Map<String, Object>> result = new ArrayList<>();
        List<Object[]> data = voiceAnalyticsRepository.countByHour();
        
        for (Object[] row : data) {
            Map<String, Object> item = new HashMap<>();
            int hour = ((Number) row[0]).intValue();
            item.put("hour", hour);
            item.put("label", String.format("%02d:00", hour));
            item.put("count", row[1]);
            result.add(item);
        }
        return result;
    }
}