package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.model.VoicePrint;
import com.vcsm.model.VoiceVerificationResponse;
import com.vcsm.repository.UserRepository;
import com.vcsm.repository.VoicePrintRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VoiceBiometricsServiceTest {

    @Mock
    private VoicePrintRepository voicePrintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VoiceFeatureService featureService;

    @Mock
    private LanguageDetectionService languageDetectionService;

    @InjectMocks
    private VoiceBiometricsService voiceBiometricsService;

    private User testUser;
    private String base64Audio;
    private double[] dummyFeatures;
    private String featuresJson;

    @BeforeEach
    public void setUp() {
        testUser = new User("resident@example.com", "John Doe", "password");
        testUser.setId(1L);

        base64Audio = Base64.getEncoder().encodeToString("audio-sample-data".getBytes());
        dummyFeatures = new double[]{0.1, 0.2, 0.3, 0.4, 0.5};
        featuresJson = "[0.1,0.2,0.3,0.4,0.5]";
    }

    @Test
    public void testEnrollVoice_SuccessEnglish() {
        String transcriptText = "My voice is my secure password";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(languageDetectionService.detectLanguage(transcriptText)).thenReturn("en");
        when(featureService.extractFeatures(any(), anyInt())).thenReturn(dummyFeatures);
        when(featureService.featuresToJson(dummyFeatures)).thenReturn(featuresJson);
        when(voicePrintRepository.findByUser(testUser)).thenReturn(Optional.empty());

        VoiceVerificationResponse response = voiceBiometricsService.enrollVoice(
            1L, base64Audio, 3.0, "en", transcriptText);

        assertTrue(response.isVerified());
        assertEquals("Voice enrollment successful! You can now use voice commands.", response.getMessage());
        assertTrue(testUser.isVoiceEnrolled());

        verify(voicePrintRepository, times(1)).save(any(VoicePrint.class));
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    public void testEnrollVoice_SuccessHindi() {
        String transcriptText = "मेरी आवाज मेरी पहचान है";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(languageDetectionService.detectLanguage(transcriptText)).thenReturn("hi");
        when(featureService.extractFeatures(any(), anyInt())).thenReturn(dummyFeatures);
        when(featureService.featuresToJson(dummyFeatures)).thenReturn(featuresJson);
        when(voicePrintRepository.findByUser(testUser)).thenReturn(Optional.empty());

        VoiceVerificationResponse response = voiceBiometricsService.enrollVoice(
            1L, base64Audio, 3.0, "hi", transcriptText);

        assertTrue(response.isVerified());
        assertEquals("Voice enrollment successful! You can now use voice commands.", response.getMessage());
        assertTrue(testUser.isVoiceEnrolled());

        verify(voicePrintRepository, times(1)).save(any(VoicePrint.class));
    }

    @Test
    public void testEnrollVoice_FailureLanguageMismatch() {
        String transcriptText = "मेरी आवाज मेरी पहचान है";
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(languageDetectionService.detectLanguage(transcriptText)).thenReturn("hi");

        VoiceVerificationResponse response = voiceBiometricsService.enrollVoice(
            1L, base64Audio, 3.0, "en", transcriptText);

        assertFalse(response.isVerified());
        assertTrue(response.getMessage().contains("Voice pattern does not match the chosen language context"));
        assertFalse(testUser.isVoiceEnrolled());

        verify(voicePrintRepository, never()).save(any(VoicePrint.class));
    }

    @Test
    public void testEnrollVoice_FailureEmptyText() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        VoiceVerificationResponse response = voiceBiometricsService.enrollVoice(
            1L, base64Audio, 3.0, "en", "");

        assertFalse(response.isVerified());
        assertEquals("Enrollment failed: Verification text is required", response.getMessage());
    }

    @Test
    public void testVerifyVoice_Success() {
        String verifyText = "My voice is my secure password";
        VoicePrint storedPrint = new VoicePrint(testUser, featuresJson, 3.0, "en");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        testUser.setVoiceEnrolled(true);
        when(voicePrintRepository.findByUser(testUser)).thenReturn(Optional.of(storedPrint));
        when(languageDetectionService.detectLanguage(verifyText)).thenReturn("en");
        when(featureService.extractFeatures(any(), anyInt())).thenReturn(dummyFeatures);
        when(featureService.jsonToFeatures(featuresJson)).thenReturn(dummyFeatures);
        when(featureService.calculateCosineSimilarity(dummyFeatures, dummyFeatures)).thenReturn(0.85);

        VoiceVerificationResponse response = voiceBiometricsService.verifyVoice(1L, base64Audio, verifyText);

        assertTrue(response.isVerified());
        assertTrue(response.getMessage().contains("Voice verified successfully!"));
    }

    @Test
    public void testVerifyVoice_FailureLanguageMismatch() {
        String verifyText = "My voice is my secure password";
        VoicePrint storedPrint = new VoicePrint(testUser, featuresJson, 3.0, "hi");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        testUser.setVoiceEnrolled(true);
        when(voicePrintRepository.findByUser(testUser)).thenReturn(Optional.of(storedPrint));
        when(languageDetectionService.detectLanguage(verifyText)).thenReturn("en");

        VoiceVerificationResponse response = voiceBiometricsService.verifyVoice(1L, base64Audio, verifyText);

        assertFalse(response.isVerified());
        assertTrue(response.getMessage().contains("does not match enrolled language context"));
    }
}
