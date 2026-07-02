package com.vcsm.controller;

import com.vcsm.emotion.ContextualEmotionAI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/multimodal")
@CrossOrigin(origins = "*")
public class MultiModalController {

    @Autowired
    private ContextualEmotionAI contextualEmotionAI;

    @PostMapping("/analyze")
    public ResponseEntity<ContextualEmotionAI.EmotionAnalysis> analyzeEmotion(
            @RequestParam String userId,
            @Valid @RequestBody MultiModalRequest request) {
        return ResponseEntity.ok(contextualEmotionAI.analyzeEmotion(
            userId,
            request.getText(),
            request.getVoiceFeatures(),
            request.getFacialFeatures()
        ));
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<ContextualEmotionAI.EmotionHistory> getHistory(@PathVariable String userId) {
        return ResponseEntity.ok(contextualEmotionAI.getEmotionHistory(userId));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(contextualEmotionAI.getEmotionStats());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "Multi-modal Emotion AI active");
        status.put("features", new String[]{
            "Text Sentiment Analysis",
            "Voice Emotion Detection",
            "Facial Expression Analysis",
            "Contextual Understanding",
            "Long-term Emotion Tracking",
            "Personalized Emotion Models"
        });
        return ResponseEntity.ok(status);
    }

    public static class MultiModalRequest {
        private String text;
        private double[] voiceFeatures;
        private double[] facialFeatures;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public double[] getVoiceFeatures() { return voiceFeatures; }
        public void setVoiceFeatures(double[] voiceFeatures) { this.voiceFeatures = voiceFeatures; }
        public double[] getFacialFeatures() { return facialFeatures; }
        public void setFacialFeatures(double[] facialFeatures) { this.facialFeatures = facialFeatures; }
    }
}
