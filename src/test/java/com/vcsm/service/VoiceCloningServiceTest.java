package com.vcsm.service;

import com.vcsm.model.AdaptiveVoiceSettings;
import com.vcsm.model.SentimentAnalysis;
import com.vcsm.model.User;
import com.vcsm.model.VoiceProfile;
import com.vcsm.repository.SentimentAnalysisRepository;
import com.vcsm.repository.VoiceProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceCloningServiceTest {

    @Mock
    private VoiceProfileRepository voiceProfileRepository;

    @Mock
    private SentimentAnalysisRepository sentimentAnalysisRepository;

    @Mock
    private VoiceToneAdapterService voiceToneAdapterService;

    @InjectMocks
    private VoiceCloningService voiceCloningService;

    private User testUser;
    private VoiceProfile testProfile;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");

        testProfile = new VoiceProfile(testUser, "Test Profile", "uploads/voices/1/sample.wav");
        testProfile.setId(1L);
        testProfile.setElevenLabsVoiceId("mock_eleven_id");
        testProfile.setActive(true);
    }

    @Test
    void testSynthesizeSpeech_WithExplicitSentiment() {
        when(voiceProfileRepository.findByUserAndActiveTrue(testUser)).thenReturn(Optional.of(testProfile));
        
        AdaptiveVoiceSettings mockSettings = new AdaptiveVoiceSettings(0.85, 0.80, 0.0, true, 0.90);
        when(voiceToneAdapterService.getAdaptiveSettings("NEGATIVE", 0.85)).thenReturn(mockSettings);

        byte[] result = voiceCloningService.synthesizeSpeech(testUser, "I am unhappy", "NEGATIVE", 0.85);

        assertNotNull(result);
        String audioStr = new String(result);
        assertTrue(audioStr.contains("Audio: 'I am unhappy'"));
        assertTrue(audioStr.contains("voice=mock_eleven_id"));
        assertTrue(audioStr.contains("stability=0.85"));
        assertTrue(audioStr.contains("speed=0.90"));
    }

    @Test
    void testSynthesizeSpeech_WithImplicitDbSentiment() {
        when(voiceProfileRepository.findByUserAndActiveTrue(testUser)).thenReturn(Optional.of(testProfile));
        
        List<SentimentAnalysis> history = new ArrayList<>();
        SentimentAnalysis sentiment = new SentimentAnalysis(testUser, "POSITIVE", 0.90, "Good job");
        history.add(sentiment);
        
        when(sentimentAnalysisRepository.findByUser(testUser)).thenReturn(history);

        AdaptiveVoiceSettings mockSettings = new AdaptiveVoiceSettings(0.60, 0.85, 0.3, true, 1.05);
        when(voiceToneAdapterService.getAdaptiveSettings("POSITIVE", 0.90)).thenReturn(mockSettings);

        byte[] result = voiceCloningService.synthesizeSpeech(testUser, "Hello there");

        assertNotNull(result);
        String audioStr = new String(result);
        assertTrue(audioStr.contains("Audio: 'Hello there'"));
        assertTrue(audioStr.contains("stability=0.60"));
        assertTrue(audioStr.contains("speed=1.05"));
    }
}
