package com.vcsm.service;

import com.vcsm.model.AdaptiveVoiceSettings;
import org.springframework.stereotype.Service;

@Service
public class VoiceToneAdapterService {

    /**
     * Map sentiment and confidence to dynamic ElevenLabs voice settings and speaking rate.
     */
    public AdaptiveVoiceSettings getAdaptiveSettings(String sentiment, Double confidence) {
        // Defaults
        double stability = 0.75;
        double similarityBoost = 0.75;
        double style = 0.0;
        boolean useSpeakerBoost = true;
        double speedFactor = 1.00;

        if (sentiment == null) {
            return new AdaptiveVoiceSettings(stability, similarityBoost, style, useSpeakerBoost, speedFactor);
        }

        double conf = (confidence != null) ? confidence : 0.5;

        // Apply empathy mapping for negative sentiments
        if ("NEGATIVE".equalsIgnoreCase(sentiment) || "VERY_NEGATIVE".equalsIgnoreCase(sentiment)) {
            if (conf > 0.7) {
                // Slower speaking rates and higher stability for high negative confidence
                speedFactor = 0.90;
                stability = 0.85; // higher stability
                similarityBoost = 0.80;
                style = 0.0; // keep it calm and steady
            } else {
                // Mild negative
                speedFactor = 0.95;
                stability = 0.80;
                similarityBoost = 0.75;
                style = 0.1;
            }
        } else if ("POSITIVE".equalsIgnoreCase(sentiment) || "VERY_POSITIVE".equalsIgnoreCase(sentiment)) {
            if (conf > 0.7) {
                // More dynamic and slightly faster speaking rate for positive sentiment
                speedFactor = 1.05;
                stability = 0.60; // lower stability allows for more emotional variance
                similarityBoost = 0.85;
                style = 0.3; // higher style variance
            } else {
                // Mild positive
                speedFactor = 1.02;
                stability = 0.65;
                similarityBoost = 0.80;
                style = 0.2;
            }
        } else if ("NEUTRAL".equalsIgnoreCase(sentiment)) {
            // Neutral/standard
            speedFactor = 1.00;
            stability = 0.75;
            similarityBoost = 0.75;
            style = 0.0;
        }

        return new AdaptiveVoiceSettings(stability, similarityBoost, style, useSpeakerBoost, speedFactor);
    }
}
