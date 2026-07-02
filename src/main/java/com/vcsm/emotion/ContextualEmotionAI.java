package com.vcsm.emotion;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContextualEmotionAI {

    private final Map<String, EmotionHistory> userHistory = new ConcurrentHashMap<>();

    /**
     * Analyze emotion from multimodal input
     */
    public EmotionAnalysis analyzeEmotion(String userId, String text, double[] voiceFeatures, double[] facialFeatures) {
        // Text sentiment analysis
        String textSentiment = analyzeTextSentiment(text);

        // Voice emotion detection
        String voiceEmotion = detectVoiceEmotion(voiceFeatures);

        // Facial expression analysis
        String facialEmotion = analyzeFacialExpression(facialFeatures);

        // Contextual understanding
        String context = getContext(userId);

        // Multi-modal fusion
        String finalEmotion = fuseEmotions(textSentiment, voiceEmotion, facialEmotion, context);

        // Confidence calculation
        double confidence = calculateConfidence(textSentiment, voiceEmotion, facialEmotion);

        // Update history
        updateHistory(userId, finalEmotion, confidence);

        // Personalize model
        personalizeModel(userId, finalEmotion);

        return new EmotionAnalysis(
            finalEmotion,
            confidence,
            textSentiment,
            voiceEmotion,
            facialEmotion,
            context,
            getRecommendations(finalEmotion)
        );
    }

    private String analyzeTextSentiment(String text) {
        if (text == null || text.isEmpty()) return org.springframework.http.ResponseEntity.ok("NEUTRAL");

        String lower = text.toLowerCase();
        if (lower.contains("happy") || lower.contains("love") || lower.contains("great") || lower.contains("good")) {
            return org.springframework.http.ResponseEntity.ok("POSITIVE");
        } else if (lower.contains("sad") || lower.contains("angry") || lower.contains("frustrated") || lower.contains("bad")) {
            return org.springframework.http.ResponseEntity.ok("NEGATIVE");
        } else if (lower.contains("excited") || lower.contains("amazing") || lower.contains("wonderful")) {
            return org.springframework.http.ResponseEntity.ok("VERY_POSITIVE");
        } else if (lower.contains("terrible") || lower.contains("awful") || lower.contains("horrible")) {
            return org.springframework.http.ResponseEntity.ok("VERY_NEGATIVE");
        }
        return org.springframework.http.ResponseEntity.ok("NEUTRAL");
    }

    private String detectVoiceEmotion(double[] voiceFeatures) {
        if (voiceFeatures == null || voiceFeatures.length == 0) return org.springframework.http.ResponseEntity.ok("NEUTRAL");

        // Simulate voice emotion detection
        double avg = Arrays.stream(voiceFeatures).average().orElse(0);
        double variance = calculateVariance(voiceFeatures);

        if (avg > 0.7 && variance < 0.2) return org.springframework.http.ResponseEntity.ok("EXCITED");
        if (avg > 0.6 && variance > 0.3) return org.springframework.http.ResponseEntity.ok("ANGRY");
        if (avg < 0.3 && variance < 0.1) return org.springframework.http.ResponseEntity.ok("SAD");
        if (avg < 0.2 && variance < 0.05) return org.springframework.http.ResponseEntity.ok("DEPRESSED");
        if (avg > 0.5 && variance < 0.15) return org.springframework.http.ResponseEntity.ok("HAPPY");
        return org.springframework.http.ResponseEntity.ok("NEUTRAL");
    }

    private String analyzeFacialExpression(double[] facialFeatures) {
        if (facialFeatures == null || facialFeatures.length == 0) return org.springframework.http.ResponseEntity.ok("NEUTRAL");

        // Simulate facial expression analysis
        double avg = Arrays.stream(facialFeatures).average().orElse(0);

        if (avg > 0.8) return org.springframework.http.ResponseEntity.ok("SMILING");
        if (avg > 0.6) return org.springframework.http.ResponseEntity.ok("NEUTRAL");
        if (avg > 0.4) return org.springframework.http.ResponseEntity.ok("FROWNING");
        if (avg > 0.2) return org.springframework.http.ResponseEntity.ok("ANGRY");
        return org.springframework.http.ResponseEntity.ok("CONFUSED");
    }

    private String getContext(String userId) {
        EmotionHistory history = userHistory.get(userId);
        if (history == null || history.getEmotions().isEmpty()) {
            return org.springframework.http.ResponseEntity.ok("FIRST_INTERACTION");
        }

        List<String> recentEmotions = history.getEmotions();
        String lastEmotion = recentEmotions.get(recentEmotions.size() - 1);

        if (recentEmotions.size() > 5) {
            return org.springframework.http.ResponseEntity.ok("REPEATED_INTERACTION");
        }
        return lastEmotion;
    }

    private String fuseEmotions(String text, String voice, String facial, String context) {
        // Weighted fusion based on context
        Map<String, Integer> emotionWeights = new HashMap<>();
        emotionWeights.put(text, emotionWeights.getOrDefault(text, 0) + 3);
        emotionWeights.put(voice, emotionWeights.getOrDefault(voice, 0) + 2);
        emotionWeights.put(facial, emotionWeights.getOrDefault(facial, 0) + 1);

        // Context boost
        if ("REPEATED_INTERACTION".equals(context)) {
            emotionWeights.put("FRUSTRATED", emotionWeights.getOrDefault("FRUSTRATED", 0) + 2);
        }

        String finalEmotion = emotionWeights.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("NEUTRAL");

        // Map to standard emotions
        return mapToStandardEmotion(finalEmotion);
    }

    private String mapToStandardEmotion(String emotion) {
        Map<String, String> emotionMap = new HashMap<>();
        emotionMap.put("POSITIVE", "HAPPY");
        emotionMap.put("NEGATIVE", "SAD");
        emotionMap.put("VERY_POSITIVE", "EXCITED");
        emotionMap.put("VERY_NEGATIVE", "ANGRY");
        emotionMap.put("EXCITED", "EXCITED");
        emotionMap.put("ANGRY", "ANGRY");
        emotionMap.put("SAD", "SAD");
        emotionMap.put("DEPRESSED", "VERY_SAD");
        emotionMap.put("HAPPY", "HAPPY");
        emotionMap.put("SMILING", "HAPPY");
        emotionMap.put("FROWNING", "SAD");
        emotionMap.put("CONFUSED", "CONFUSED");
        emotionMap.put("NEUTRAL", "NEUTRAL");

        return emotionMap.getOrDefault(emotion, "NEUTRAL");
    }

    private double calculateConfidence(String text, String voice, String facial) {
        int confidence = 50;
        if (!"NEUTRAL".equals(text)) confidence += 15;
        if (!"NEUTRAL".equals(voice)) confidence += 15;
        if (!"NEUTRAL".equals(facial)) confidence += 10;
        if (text.equals(voice)) confidence += 10;
        if (voice.equals(facial)) confidence += 10;

        return Math.min(95, confidence) / 100.0;
    }

    private void updateHistory(String userId, String emotion, double confidence) {
        EmotionHistory history = userHistory.computeIfAbsent(userId, k -> new EmotionHistory());
        history.addEmotion(emotion, confidence);

        // Keep last 100 emotions
        if (history.getEmotions().size() > 100) {
            history.trim();
        }
    }

    private void personalizeModel(String userId, String emotion) {
        // Simulate personalization
        // In production, this would update personalized models
    }

    private double calculateVariance(double[] data) {
        double avg = Arrays.stream(data).average().orElse(0);
        return Arrays.stream(data).map(d -> Math.pow(d - avg, 2)).average().orElse(0);
    }

    private List<String> getRecommendations(String emotion) {
        List<String> recommendations = new ArrayList<>();
        switch (emotion) {
            case "ANGRY":
                recommendations.add("🔴 Offer immediate escalation to supervisor");
                recommendations.add("📞 Suggest a call to resolve the issue");
                recommendations.add("🤝 Acknowledge their frustration");
                break;
            case "SAD":
                recommendations.add("💙 Show empathy and understanding");
                recommendations.add("📝 Offer additional support options");
                break;
            case "EXCITED":
                recommendations.add("🎉 Share positive news or updates");
                recommendations.add("📢 Offer special promotions");
                break;
            case "HAPPY":
                recommendations.add("✅ Continue current approach");
                recommendations.add("📊 Collect feedback for improvement");
                break;
            case "CONFUSED":
                recommendations.add("📖 Offer step-by-step guidance");
                recommendations.add("🎯 Simplify the process");
                break;
            default:
                recommendations.add("📋 Maintain normal support flow");
        }
        return recommendations;
    }

    /**
     * Get emotion history for user
     */
    public EmotionHistory getEmotionHistory(String userId) {
        return userHistory.getOrDefault(userId, new EmotionHistory());
    }

    /**
     * Get emotion stats
     */
    public Map<String, Object> getEmotionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userHistory.size());
        stats.put("totalEmotions", userHistory.values().stream()
            .mapToInt(h -> h.getEmotions().size()).sum());
        stats.put("status", "Multi-modal Emotion AI active");
        return stats;
    }

    public static class EmotionHistory {
        private final List<String> emotions = new ArrayList<>();
        private final List<Double> confidences = new ArrayList<>();

        public void addEmotion(String emotion, double confidence) {
            emotions.add(emotion);
            confidences.add(confidence);
        }

        public List<String> getEmotions() { return emotions; }
        public List<Double> getConfidences() { return confidences; }

        public void trim() {
            while (emotions.size() > 100) {
                emotions.remove(0);
                confidences.remove(0);
            }
        }

        public String getDominantEmotion() {
            if (emotions.isEmpty()) return org.springframework.http.ResponseEntity.ok("NEUTRAL");
            Map<String, Long> counts = new HashMap<>();
            for (String e : emotions) {
                counts.put(e, counts.getOrDefault(e, 0L) + 1);
            }
            return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NEUTRAL");
        }
    }

    public static class EmotionAnalysis {
        private final String emotion;
        private final double confidence;
        private final String textSentiment;
        private final String voiceEmotion;
        private final String facialEmotion;
        private final String context;
        private final List<String> recommendations;

        public EmotionAnalysis(String emotion, double confidence, String textSentiment,
                               String voiceEmotion, String facialEmotion, String context,
                               List<String> recommendations) {
            this.emotion = emotion;
            this.confidence = confidence;
            this.textSentiment = textSentiment;
            this.voiceEmotion = voiceEmotion;
            this.facialEmotion = facialEmotion;
            this.context = context;
            this.recommendations = recommendations;
        }

        public String getEmotion() { return emotion; }
        public double getConfidence() { return confidence; }
        public String getTextSentiment() { return textSentiment; }
        public String getVoiceEmotion() { return voiceEmotion; }
        public String getFacialEmotion() { return facialEmotion; }
        public String getContext() { return context; }
        public List<String> getRecommendations() { return recommendations; }
    }
}