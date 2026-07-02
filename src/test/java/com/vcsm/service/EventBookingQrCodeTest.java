package com.vcsm.service;

import com.vcsm.controller.EventController;
import com.vcsm.model.*;
import com.vcsm.repository.*;
import com.vcsm.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@lombok.extern.slf4j.Slf4j
public class EventBookingQrCodeTest {

    @Autowired
    private QRCodeService qrCodeService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OmnidimService omnidimService;

    @Autowired
    private EventController eventController;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private EmailQueueRepository emailQueueRepository;

    private Event testEvent;
    private User testUser;

    @BeforeEach
    void setUp() {
        emailQueueRepository.deleteAll();
        eventRegistrationRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        // Create user
        testUser = new User();
        testUser.setName("John Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPassword("hashedpassword");
        testUser.setEmailNotifications(true);
        testUser = userRepository.saveAndFlush(testUser);

        // Create event
        testEvent = new Event();
        testEvent.setName("Summer Festival");
        testEvent.setDescription("Annual community summer festival");
        testEvent.setCategory(Event.EventCategory.CULTURAL);
        testEvent.setLocation("Central Park");
        testEvent.setEventDate(LocalDateTime.now().plusDays(5));
        testEvent.setMaxCapacity(100);
        testEvent.setRegistrations(0);
        testEvent.setActive(true);
        testEvent = eventRepository.saveAndFlush(testEvent);
    }

    @Test
    void testQRCodeGeneration() throws Exception {
        byte[] qrBytes = qrCodeService.generateQRCodeImage("TICKET-TOKEN-12345", 200, 200);
        assertNotNull(qrBytes);
        assertTrue(qrBytes.length > 0);
    }

    @Test
    void testJwtTicketToken() {
        String token = jwtService.generateTicketToken(10L, 20L, 30L);
        assertNotNull(token);

        io.jsonwebtoken.Claims claims = jwtService.parseTicketToken(token);
        assertEquals(10L, claims.get("registrationId", Long.class));
        assertEquals(20L, claims.get("userId", Long.class));
        assertEquals(30L, claims.get("eventId", Long.class));
    }

    @Test
    void testVoiceDrivenEventBookingAndVerification() {
        log.info("DEBUG - Saved testUser ID: " + testUser.getId() + ", Email: " + testUser.getEmail());
        log.info("DEBUG - All users in DB: ");
        userRepository.findAll().forEach(u -> log.info("  ID: " + u.getId() + ", Email: " + u.getEmail()));

        // Reset the thread-local SecurityContext to prevent leaked mocks from other tests
        org.springframework.security.core.context.SecurityContextHolder.setContext(new org.springframework.security.core.context.SecurityContextImpl());

        // Set mock authentication context using CustomUserDetails
        com.vcsm.security.service.CustomUserDetails principal = new com.vcsm.security.service.CustomUserDetails(
            testUser.getId(),
            testUser.getEmail(),
            testUser.getPassword(),
            java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, "hashedpassword", principal.getAuthorities()
            )
        );

        org.springframework.security.core.Authentication debugAuth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        log.info("DEBUG - Auth: " + debugAuth);
        log.info("DEBUG - Auth name: " + (debugAuth != null ? debugAuth.getName() : "null"));

        // 1. Process Voice Command
        String transcript = "I want to book a ticket for the summer festival, please";
        Map<String, Object> result = omnidimService.processVoiceCommand(transcript);

        assertEquals("BOOK_EVENT", result.get("intent"));
        String response = (String) result.get("response");
        log.info("DEBUG - Voice Command Response: " + response);
        assertTrue(response.contains("Success") || response.contains("registered for Summer Festival"), "Response was: " + response);

        // 2. Verify Registration was created
        Optional<EventRegistration> regOpt = eventRegistrationRepository.findByUserAndEvent(testUser, testEvent);
        assertTrue(regOpt.isPresent());
        EventRegistration reg = regOpt.get();
        assertNotNull(reg.getTicketToken());
        assertFalse(reg.isCheckedIn());

        // 3. Verify Email confirmation is queued
        long emailCount = emailQueueRepository.count();
        assertTrue(emailCount > 0);

        // 4. Verify Ticket Check-In via Controller
        String token = reg.getTicketToken();

        // Non-admin user should be denied access
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            eventController.verifyTicket(token);
        });

        // Switch to admin authentication context
        com.vcsm.security.service.CustomUserDetails adminPrincipal = new com.vcsm.security.service.CustomUserDetails(
            999L,
            "admin@example.com",
            "adminpassword",
            java.util.Collections.singletonList(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
            new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                adminPrincipal, "adminpassword", adminPrincipal.getAuthorities()
            )
        );

        // Now verification should succeed
        ResponseEntity<?> verifyResponse = eventController.verifyTicket(token);
        assertEquals(200, verifyResponse.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) verifyResponse.getBody();
        assertNotNull(body);
        assertEquals(true, body.get("valid"));
        assertEquals(false, body.get("alreadyCheckedIn"));

        // Reload registration
        reg = eventRegistrationRepository.findById(reg.getId()).orElseThrow();
        assertTrue(reg.isCheckedIn());
        assertNotNull(reg.getCheckedInAt());

        // 5. Double Check-In scan should return alreadyCheckedIn = true
        ResponseEntity<?> verifyAgainResponse = eventController.verifyTicket(token);
        assertEquals(200, verifyAgainResponse.getStatusCode().value());

        Map<?, ?> bodyAgain = (Map<?, ?>) verifyAgainResponse.getBody();
        assertNotNull(bodyAgain);
        assertEquals(true, bodyAgain.get("valid"));
        assertEquals(true, bodyAgain.get("alreadyCheckedIn"));
    }
}
