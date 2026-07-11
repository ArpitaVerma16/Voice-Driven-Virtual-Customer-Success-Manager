package com.vcsm.controller;

import com.vcsm.bci.BCIService;
import com.vcsm.security.service.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BCIControllerTest {

    private BCIController controller;
    private BCIService bciService;

    private CustomUserDetails user1Details;
    private CustomUserDetails user2Details;
    private CustomUserDetails adminDetails;

    @BeforeEach
    void setUp() {
        bciService = new BCIService();
        controller = new BCIController();
        
        // Inject BCIService manually
        try {
            java.lang.reflect.Field field = BCIController.class.getDeclaredField("bciService");
            field.setAccessible(true);
            field.set(controller, bciService);
        } catch (Exception e) {
            fail("Failed to inject BCIService: " + e.getMessage());
        }

        user1Details = new CustomUserDetails(
                1L,
                "user1",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        user2Details = new CustomUserDetails(
                2L,
                "user2",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        adminDetails = new CustomUserDetails(
                3L,
                "admin",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    void startSession_Unauthenticated_ReturnsUnauthorized() {
        ResponseEntity<?> response = controller.startSession("user1", null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Authentication required", response.getBody());
    }

    @Test
    void startSession_Owner_ReturnsOk() {
        ResponseEntity<?> response = controller.startSession("user1", user1Details);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof BCIService.BCISession);
        assertEquals("user1", ((BCIService.BCISession) response.getBody()).getUserId());
    }

    @Test
    void startSession_DefaultToUsername_ReturnsOk() {
        ResponseEntity<?> response = controller.startSession(null, user1Details);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("user1", ((BCIService.BCISession) response.getBody()).getUserId());
    }

    @Test
    void startSession_DifferentUser_ReturnsForbidden() {
        ResponseEntity<?> response = controller.startSession("user2", user1Details);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void startSession_Admin_ReturnsOk() {
        ResponseEntity<?> response = controller.startSession("user2", adminDetails);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("user2", ((BCIService.BCISession) response.getBody()).getUserId());
    }

    @Test
    void getHistory_Unauthenticated_ReturnsUnauthorized() {
        ResponseEntity<?> response = controller.getHistory("user1", null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void getHistory_Owner_ReturnsOk() {
        bciService.startSession("user1");
        ResponseEntity<?> response = controller.getHistory("user1", user1Details);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getHistory_DifferentUser_ReturnsForbidden() {
        bciService.startSession("user2");
        ResponseEntity<?> response = controller.getHistory("user2", user1Details);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void getHistory_Admin_ReturnsOk() {
        bciService.startSession("user2");
        ResponseEntity<?> response = controller.getHistory("user2", adminDetails);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void sessionActions_ForbiddenForNonOwner() {
        BCIService.BCISession session = bciService.startSession("user2");
        String sessionId = session.getSessionId();

        // processSignal
        ResponseEntity<?> signalResponse = controller.processSignal(sessionId, new double[]{0.1, 0.2}, user1Details);
        assertEquals(HttpStatus.FORBIDDEN, signalResponse.getStatusCode());

        // getSession
        ResponseEntity<?> getSessionResponse = controller.getSession(sessionId, user1Details);
        assertEquals(HttpStatus.FORBIDDEN, getSessionResponse.getStatusCode());

        // endSession
        ResponseEntity<?> endSessionResponse = controller.endSession(sessionId, user1Details);
        assertEquals(HttpStatus.FORBIDDEN, endSessionResponse.getStatusCode());
    }

    @Test
    void sessionActions_AllowedForOwner() {
        BCIService.BCISession session = bciService.startSession("user1");
        String sessionId = session.getSessionId();

        // processSignal
        ResponseEntity<?> signalResponse = controller.processSignal(sessionId, new double[]{0.1, 0.2}, user1Details);
        assertEquals(HttpStatus.OK, signalResponse.getStatusCode());

        // getSession
        ResponseEntity<?> getSessionResponse = controller.getSession(sessionId, user1Details);
        assertEquals(HttpStatus.OK, getSessionResponse.getStatusCode());

        // endSession
        ResponseEntity<?> endSessionResponse = controller.endSession(sessionId, user1Details);
        assertEquals(HttpStatus.OK, endSessionResponse.getStatusCode());
    }

    @Test
    void sessionActions_AllowedForAdmin() {
        BCIService.BCISession session = bciService.startSession("user1");
        String sessionId = session.getSessionId();

        // processSignal
        ResponseEntity<?> signalResponse = controller.processSignal(sessionId, new double[]{0.1, 0.2}, adminDetails);
        assertEquals(HttpStatus.OK, signalResponse.getStatusCode());

        // getSession
        ResponseEntity<?> getSessionResponse = controller.getSession(sessionId, adminDetails);
        assertEquals(HttpStatus.OK, getSessionResponse.getStatusCode());

        // endSession
        ResponseEntity<?> endSessionResponse = controller.endSession(sessionId, adminDetails);
        assertEquals(HttpStatus.OK, endSessionResponse.getStatusCode());
    }
}
