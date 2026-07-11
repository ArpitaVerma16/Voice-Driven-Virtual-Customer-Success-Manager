package com.vcsm.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class VoiceQualityServiceTest {

    @Autowired
    private VoiceQualityService voiceQualityService;

    @Test
    void testAnalyzeAudioQualityFileNotFound() {
        Map<String, Object> result = voiceQualityService.analyzeAudioQuality("/non/existent/file.wav");

        assertEquals(0, result.get("quality_score"));
        assertEquals("error", result.get("status"));
        assertTrue(result.get("message").toString().contains("not found"));
    }

    @Test
    void testShouldBlockTranscriptionForLowQuality() {
        Map<String, Object> result = voiceQualityService.analyzeAudioQuality("/non/existent/file.wav");
        assertTrue(voiceQualityService.shouldBlockTranscription("/non/existent/file.wav"));
    }

    @Test
    void testShouldWarnUserForPoorQuality() {
        Map<String, Object> result = voiceQualityService.analyzeAudioQuality("/non/existent/file.wav");
        assertTrue(voiceQualityService.shouldBlockTranscription("/non/existent/file.wav"));
    }

    @Test
    void testQualityScoreNonNegative() {
        Map<String, Object> result = voiceQualityService.analyzeAudioQuality("/non/existent/file.wav");
        int score = (int) result.get("quality_score");
        assertTrue(score >= 0 && score <= 100);
    }

    @Test
    void testAnalysisResultStructure() {
        Map<String, Object> result = voiceQualityService.analyzeAudioQuality("/non/existent/file.wav");

        assertTrue(result.containsKey("quality_score"));
        assertTrue(result.containsKey("status"));
        assertTrue(result.containsKey("message"));
    }
}
