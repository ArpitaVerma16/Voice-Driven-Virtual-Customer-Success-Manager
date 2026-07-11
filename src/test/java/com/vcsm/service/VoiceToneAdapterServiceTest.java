package com.vcsm.service;

import com.vcsm.model.AdaptiveVoiceSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VoiceToneAdapterServiceTest {

    private VoiceToneAdapterService adapterService;

    @BeforeEach
    void setUp() {
        adapterService = new VoiceToneAdapterService();
    }

    @Test
    void testGetAdaptiveSettings_DefaultNullSentiment() {
        AdaptiveVoiceSettings settings = adapterService.getAdaptiveSettings(null, null);
        assertNotNull(settings);
        assertEquals(0.75, settings.getStability(), 0.001);
        assertEquals(0.75, settings.getSimilarityBoost(), 0.001);
        assertEquals(0.0, settings.getStyle(), 0.001);
        assertTrue(settings.isUseSpeakerBoost());
        assertEquals(1.00, settings.getSpeedFactor(), 0.001);
    }

    @Test
    void testGetAdaptiveSettings_HighNegativeSentiment() {
        AdaptiveVoiceSettings settings = adapterService.getAdaptiveSettings("NEGATIVE", 0.85);
        assertNotNull(settings);
        assertEquals(0.85, settings.getStability(), 0.001);
        assertEquals(0.80, settings.getSimilarityBoost(), 0.001);
        assertEquals(0.0, settings.getStyle(), 0.001);
        assertTrue(settings.isUseSpeakerBoost());
        assertEquals(0.90, settings.getSpeedFactor(), 0.001);
    }

    @Test
    void testGetAdaptiveSettings_MildNegativeSentiment() {
        AdaptiveVoiceSettings settings = adapterService.getAdaptiveSettings("NEGATIVE", 0.50);
        assertNotNull(settings);
        assertEquals(0.80, settings.getStability(), 0.001);
        assertEquals(0.75, settings.getSimilarityBoost(), 0.001);
        assertEquals(0.1, settings.getStyle(), 0.001);
        assertTrue(settings.isUseSpeakerBoost());
        assertEquals(0.95, settings.getSpeedFactor(), 0.001);
    }

    @Test
    void testGetAdaptiveSettings_HighPositiveSentiment() {
        AdaptiveVoiceSettings settings = adapterService.getAdaptiveSettings("POSITIVE", 0.90);
        assertNotNull(settings);
        assertEquals(0.60, settings.getStability(), 0.001);
        assertEquals(0.85, settings.getSimilarityBoost(), 0.001);
        assertEquals(0.3, settings.getStyle(), 0.001);
        assertTrue(settings.isUseSpeakerBoost());
        assertEquals(1.05, settings.getSpeedFactor(), 0.001);
    }

    @Test
    void testGetAdaptiveSettings_MildPositiveSentiment() {
        AdaptiveVoiceSettings settings = adapterService.getAdaptiveSettings("POSITIVE", 0.60);
        assertNotNull(settings);
        assertEquals(0.65, settings.getStability(), 0.001);
        assertEquals(0.80, settings.getSimilarityBoost(), 0.001);
        assertEquals(0.2, settings.getStyle(), 0.001);
        assertTrue(settings.isUseSpeakerBoost());
        assertEquals(1.02, settings.getSpeedFactor(), 0.001);
    }

    @Test
    void testGetAdaptiveSettings_NeutralSentiment() {
        AdaptiveVoiceSettings settings = adapterService.getAdaptiveSettings("NEUTRAL", 0.99);
        assertNotNull(settings);
        assertEquals(0.75, settings.getStability(), 0.001);
        assertEquals(0.75, settings.getSimilarityBoost(), 0.001);
        assertEquals(0.0, settings.getStyle(), 0.001);
        assertTrue(settings.isUseSpeakerBoost());
        assertEquals(1.00, settings.getSpeedFactor(), 0.001);
    }
}
