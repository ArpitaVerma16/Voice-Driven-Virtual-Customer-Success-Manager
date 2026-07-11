package com.vcsm.controller;

import com.vcsm.security.FraudAlertService;
import com.vcsm.service.VoiceOtpService;
import com.vcsm.service.VoiceOtpService.VoiceOtpSession;
import com.vcsm.service.VoiceOtpService.VoiceOtpSessionVerificationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityControllerMfaTest {

    @Mock
    private VoiceOtpService voiceOtpService;

    @Mock
    private FraudAlertService fraudAlertService;

    @InjectMocks
    private SecurityController securityController;

    private Long userId = 1L;
    private String sessionId = "session-uuid";
    private String voiceSample = "base64-audio";

    @Test
    public void testCreateOtpChallenge() {
        VoiceOtpSession mockSession = new VoiceOtpSession(sessionId, userId, "1234");
        when(voiceOtpService.generateChallenge(userId)).thenReturn(mockSession);

        ResponseEntity<VoiceOtpSession> response = securityController.createOtpChallenge(userId);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertEquals(mockSession, response.getBody());
    }

    @Test
    public void testVerifyOtp_Success() {
        VoiceOtpSessionVerificationResult mockResult = new VoiceOtpSessionVerificationResult(
            true, "Verified", "1234", 0.85, userId
        );
        when(voiceOtpService.verifyOtp(sessionId, voiceSample)).thenReturn(mockResult);

        ResponseEntity<SecurityController.MfaVerificationResult> response = securityController.verifyOtp(sessionId, voiceSample);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().isVerified());
        assertEquals("Verified", response.getBody().getMessage());
        assertEquals("1234", response.getBody().getTranscript());
        assertEquals(0.85, response.getBody().getBiometricsScore());

        // Ensure no fraud alert is raised
        verify(fraudAlertService, never()).raiseAlert(anyString(), anyString(), anyString());
    }

    @Test
    public void testVerifyOtp_FailureRaisesAlert() {
        VoiceOtpSessionVerificationResult mockResult = new VoiceOtpSessionVerificationResult(
            false, "Failed", "9999", 0.30, userId
        );
        when(voiceOtpService.verifyOtp(sessionId, voiceSample)).thenReturn(mockResult);

        ResponseEntity<SecurityController.MfaVerificationResult> response = securityController.verifyOtp(sessionId, voiceSample);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertFalse(response.getBody().isVerified());
        assertEquals("Failed", response.getBody().getMessage());

        // Ensure fraud alert IS raised
        verify(fraudAlertService, times(1)).raiseAlert(
            eq("1"),
            eq("Voice MFA OTP verification failed"),
            eq("HIGH")
        );
    }
}
