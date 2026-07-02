package com.vcsm.controller;

import com.vcsm.service.VoiceAnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics/voice")
public class VoiceAnalyticsController {
    
    @Autowired
    private VoiceAnalyticsService voiceAnalyticsService;
    
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(voiceAnalyticsService.getSummary());
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        return ResponseEntity.ok(voiceAnalyticsService.getAnalytics());
    }
    
    @GetMapping("/commands")
    public ResponseEntity<List<Map<String, Object>>> getCommandStats() {
        return ResponseEntity.ok(voiceAnalyticsService.getCommandStats());
    }
    
    @GetMapping("/hourly")
    public ResponseEntity<List<Map<String, Object>>> getHourlyStats() {
        return ResponseEntity.ok(voiceAnalyticsService.getHourlyStats());
    }
}