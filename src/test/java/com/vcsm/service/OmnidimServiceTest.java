package com.vcsm.service;

import com.vcsm.model.Event;
import com.vcsm.model.User;
import com.vcsm.model.VoiceCommand;
import com.vcsm.repository.UserRepository;
import com.vcsm.repository.VoiceCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OmnidimServiceTest {

    @Mock
    private VoiceCommandRepository voiceCommandRepository;

    @Mock
    private ComplaintService complaintService;

    @Mock
    private EventService eventService;

    @Mock
    private EventRegistrationService eventRegistrationService;

    @Mock
    private VoiceModelRegistryService voiceModelRegistryService;

    @Mock
    private VoiceAnalyticsService voiceAnalyticsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private OmnidimService omnidimService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testProcessVoiceCommandCancelRegistration() {
        String transcript = "please cancel registration for sports day";
        
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("resident@example.com");

        User user = new User();
        user.setId(1L);
        user.setName("Resident");
        user.setEmail("resident@example.com");
        when(userRepository.findByEmail("resident@example.com")).thenReturn(Optional.of(user));

        Event event = new Event();
        event.setId(5L);
        event.setName("Sports Day");
        List<Event> userEvents = new ArrayList<>();
        userEvents.add(event);
        when(eventRegistrationService.getUserEvents(user)).thenReturn(userEvents);

        when(voiceModelRegistryService.getActiveModel()).thenReturn(Optional.empty());

        Map<String, Object> result = omnidimService.processVoiceCommand(transcript);

        assertNotNull(result);
        assertEquals("CANCEL_REGISTRATION", result.get("intent"));
        assertEquals("Successfully cancelled your registration for the event: Sports Day", result.get("response"));
        assertTrue((Boolean) result.get("success"));

        verify(eventRegistrationService, times(1)).cancelRegistration(event, user);
        verify(voiceCommandRepository, times(1)).save(any(VoiceCommand.class));
    }
}
