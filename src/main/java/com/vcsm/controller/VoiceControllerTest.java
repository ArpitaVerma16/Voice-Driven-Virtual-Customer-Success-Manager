package com.vcsm.controller;

import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.HindiCommandMapper;
import com.vcsm.service.LanguageDetectionService;
import com.vcsm.service.OmnidimService;
import com.vcsm.service.SentimentAnalysisService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class VoiceControllerTest {

    private VoiceController controller;
    private OmnidimService omnidimService;
    private SentimentAnalysisService sentimentService;
    private UserRepository userRepository;
    private LanguageDetectionService languageDetectionService;

    @BeforeEach
    void setUp() {
        controller = new VoiceController();

        omnidimService = mock(OmnidimService.class);
        sentimentService = mock(SentimentAnalysisService.class);
        userRepository = mock(UserRepository.class);
        languageDetectionService = mock(LanguageDetectionService.class);

        ReflectionTestUtils.setField(
                controller,
                "omnidimService",
                omnidimService
        );

        ReflectionTestUtils.setField(
                controller,
                "sentimentService",
                sentimentService
        );

        ReflectionTestUtils.setField(
                controller,
                "userRepository",
                userRepository
        );

        ReflectionTestUtils.setField(
                controller,
                "languageDetectionService",
                languageDetectionService
        );

        ReflectionTestUtils.setField(
                controller,
                "hindiCommandMapper",
                mock(HindiCommandMapper.class)
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectEmptyTranscript() {
        ResponseEntity<Map<String, Object>> response =
                controller.command(Map.of("transcript", ""));

        assertEquals(400, response.getStatusCode().value());
        assertEquals(
                "Transcript required",
                response.getBody().get("error")
        );

        verifyNoInteractions(languageDetectionService);
        verifyNoInteractions(sentimentService);
        verifyNoInteractions(omnidimService);
    }

    @Test
    void shouldRejectOversizedTranscriptBeforeProcessing() {
        String transcript = "a".repeat(2001);

        ResponseEntity<Map<String, Object>> response =
                controller.command(
                        Map.of("transcript", transcript)
                );

        assertEquals(400, response.getStatusCode().value());
        assertEquals(
                "Transcript too long",
                response.getBody().get("error")
        );

        verifyNoInteractions(languageDetectionService);
        verifyNoInteractions(sentimentService);
        verifyNoInteractions(omnidimService);
    }

    @Test
    void shouldAcceptValidTranscript() {
        String email = "user@example.com";
        String transcript = "Show upcoming events";

        User user = new User();
        user.setId(1L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        null
                )
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(languageDetectionService.detectLanguage(transcript))
                .thenReturn("en");

        when(omnidimService.processVoiceCommand(transcript))
                .thenReturn(
                        Map.of(
                                "response",
                                "Upcoming events available"
                        )
                );

        ResponseEntity<Map<String, Object>> response =
                controller.command(
                        Map.of("transcript", transcript)
                );

        assertEquals(200, response.getStatusCode().value());
        assertEquals(
                true,
                response.getBody().get("success")
        );

        verify(languageDetectionService)
                .detectLanguage(transcript);

        verify(omnidimService)
                .processVoiceCommand(transcript);

        verify(sentimentService)
                .analyzeAndProcess(1L, transcript);
    }

    @Test
    void shouldAcceptTranscriptAtMaximumLength() {
        String email = "user@example.com";
        String transcript = "a".repeat(2000);

        User user = new User();
        user.setId(1L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        email,
                        null
                )
        );

        when(userRepository.findByEmail(email))
                .thenReturn(Optional.of(user));

        when(languageDetectionService.detectLanguage(transcript))
                .thenReturn("en");

        when(omnidimService.processVoiceCommand(transcript))
                .thenReturn(Map.of("response", "Processed"));

        ResponseEntity<Map<String, Object>> response =
                controller.command(
                        Map.of("transcript", transcript)
                );

        assertEquals(200, response.getStatusCode().value());

        verify(languageDetectionService)
                .detectLanguage(transcript);

        verify(sentimentService)
                .analyzeAndProcess(anyLong(), anyString());
    }
}