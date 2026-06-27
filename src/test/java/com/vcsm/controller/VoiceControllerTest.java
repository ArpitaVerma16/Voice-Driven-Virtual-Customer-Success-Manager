package com.vcsm.controller;

import com.vcsm.model.User;
import com.vcsm.model.Event;
import com.vcsm.service.LanguageDetectionService;
import com.vcsm.service.HindiCommandMapper;
import com.vcsm.service.OmnidimService;
import com.vcsm.service.SentimentAnalysisService;
import com.vcsm.service.EventRegistrationService;
import com.vcsm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoiceControllerTest {

    @Mock
    private OmnidimService omnidimService;

    @Mock
    private SentimentAnalysisService sentimentService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LanguageDetectionService languageDetectionService;

    @Mock
    private HindiCommandMapper hindiCommandMapper;

    @Mock
    private EventRegistrationService eventRegistrationService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private VoiceController voiceController;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCommandHindiCancelRegistration() {
        Map<String, String> body = new HashMap<>();
        body.put("transcript", "रजिस्ट्रेशन कैंसिल करो");

        when(languageDetectionService.detectLanguage("रजिस्ट्रेशन कैंसिल करो")).thenReturn("hi");
        when(hindiCommandMapper.mapCommand("रजिस्ट्रेशन कैंसिल करो")).thenReturn("cancel_registration");
        when(hindiCommandMapper.getResponse("cancel_registration", ": Annual Party")).thenReturn("आपका रजिस्ट्रेशन रद्द कर दिया गया है।: Annual Party");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("resident@example.com");

        User user = new User();
        user.setId(1L);
        user.setName("Resident");
        user.setEmail("resident@example.com");
        when(userRepository.findByEmail("resident@example.com")).thenReturn(Optional.of(user));

        Event event = new Event();
        event.setId(10L);
        event.setName("Annual Party");
        List<Event> userEvents = new ArrayList<>();
        userEvents.add(event);
        when(eventRegistrationService.getUserEvents(user)).thenReturn(userEvents);

        ResponseEntity<?> responseEntity = voiceController.command(body);

        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());
        Map<?, ?> response = (Map<?, ?>) responseEntity.getBody();
        assertNotNull(response);
        assertEquals("hi", response.get("detectedLanguage"));
        assertEquals("cancel_registration", response.get("action"));
        assertEquals("आपका रजिस्ट्रेशन रद्द कर दिया गया है।: Annual Party", response.get("response"));
        assertTrue((Boolean) response.get("success"));

        verify(eventRegistrationService, times(1)).cancelRegistration(event, user);
    }
}
