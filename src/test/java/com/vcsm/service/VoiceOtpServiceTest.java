package com.vcsm.service;

import com.vcsm.model.VoiceVerificationResponse;
import com.vcsm.service.VoiceOtpService.VoiceOtpSession;
import com.vcsm.service.VoiceOtpService.VoiceOtpSessionVerificationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VoiceOtpServiceTest {

    @Mock
    private SpeechToTextService speechToTextService;

    @Mock
    private VoiceBiometricsService voiceBiometricsService;

    @InjectMocks
    private VoiceOtpService voiceOtpService;

    private Long userId = 1L;
    private String base64Audio;

    @BeforeEach
    public void setUp() {
        base64Audio = Base64.getEncoder().encodeToString("mock-otp:1234".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void testGenerateChallenge() {
        VoiceOtpSession session = voiceOtpService.generateChallenge(userId);
        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertEquals(userId, session.getUserId());
        assertEquals(4, session.getOtpCode().length());
        assertTrue(session.getOtpCode().matches("\\d{4}"));
        assertEquals("PENDING", session.getStatus());
        assertFalse(session.isExpired());
    }

    @Test
    public void testVerifyOtp_Success() {
        VoiceOtpSession session = voiceOtpService.generateChallenge(userId);
        String otpCode = session.getOtpCode();
        String spacedOtp = otpCode.charAt(0) + " " + otpCode.charAt(1) + " " + otpCode.charAt(2) + " " + otpCode.charAt(3);

        when(speechToTextService.transcribe(eq(base64Audio), eq("en-US"))).thenReturn(otpCode);
        when(voiceBiometricsService.verifyVoice(eq(userId), eq(base64Audio), eq(spacedOtp)))
                .thenReturn(new VoiceVerificationResponse(true, 0.85, "Verified", userId, "User"));

        VoiceOtpSessionVerificationResult result = voiceOtpService.verifyOtp(session.getSessionId(), base64Audio);

        assertTrue(result.isVerified());
        assertEquals("Voice MFA verification successful", result.getMessage());
        assertEquals(otpCode, result.getTranscript());
        assertEquals(0.85, result.getBiometricsScore());
        assertEquals(userId, result.getUserId());
        assertNull(voiceOtpService.getSession(session.getSessionId())); // Session should be invalidated
    }

    @Test
    public void testVerifyOtp_IncorrectCode() {
        VoiceOtpSession session = voiceOtpService.generateChallenge(userId);

        when(speechToTextService.transcribe(eq(base64Audio), eq("en-US"))).thenReturn("9999");

        VoiceOtpSessionVerificationResult result = voiceOtpService.verifyOtp(session.getSessionId(), base64Audio);

        assertFalse(result.isVerified());
        assertTrue(result.getMessage().contains("Incorrect code spoken"));
        assertEquals("9999", result.getTranscript());
        assertEquals(0.0, result.getBiometricsScore());
        assertNull(voiceOtpService.getSession(session.getSessionId()));
    }

    @Test
    public void testVerifyOtp_BiometricsFailed() {
        VoiceOtpSession session = voiceOtpService.generateChallenge(userId);
        String otpCode = session.getOtpCode();
        String spacedOtp = otpCode.charAt(0) + " " + otpCode.charAt(1) + " " + otpCode.charAt(2) + " " + otpCode.charAt(3);

        when(speechToTextService.transcribe(eq(base64Audio), eq("en-US"))).thenReturn(otpCode);
        when(voiceBiometricsService.verifyVoice(eq(userId), eq(base64Audio), eq(spacedOtp)))
                .thenReturn(new VoiceVerificationResponse(false, 0.50, "Verification failed", userId, "User"));

        VoiceOtpSessionVerificationResult result = voiceOtpService.verifyOtp(session.getSessionId(), base64Audio);

        assertFalse(result.isVerified());
        assertTrue(result.getMessage().contains("Voice biometrics matching failed"));
        assertEquals(otpCode, result.getTranscript());
        assertEquals(0.50, result.getBiometricsScore());
        assertNull(voiceOtpService.getSession(session.getSessionId()));
    }

    @Test
    public void testVerifyOtp_SessionNotFound() {
        VoiceOtpSessionVerificationResult result = voiceOtpService.verifyOtp("non-existent-id", base64Audio);
        assertFalse(result.isVerified());
        assertEquals("Session not found", result.getMessage());
    }

    @Test
    public void testNormalizeToDigits() {
        assertEquals("1234", voiceOtpService.normalizeToDigits("one two three four"));
        assertEquals("0591", voiceOtpService.normalizeToDigits("zero five nine one"));
        assertEquals("123", voiceOtpService.normalizeToDigits("एक दो तीन"));
        assertEquals("1234", voiceOtpService.normalizeToDigits("1 2 3 4"));
    }
}
