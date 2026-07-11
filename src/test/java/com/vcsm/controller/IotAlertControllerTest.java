package com.vcsm.controller;

import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.TwilioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IotAlertControllerTest {

    @Mock
    private ComplaintRepository complaintRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TwilioService twilioService;

    @InjectMocks
    private IotAlertController iotAlertController;

    private User testUser;
    private IotAlertController.IotAlertPayload validPayload;

    @BeforeEach
    public void setUp() {
        testUser = new User("resident@example.com", "John Doe", "password");
        testUser.setId(1L);
        testUser.setPhone("+1234567890");

        validPayload = new IotAlertController.IotAlertPayload();
        validPayload.setSensorId("leak-001");
        validPayload.setSensorType("WATER_LEAK");
        validPayload.setReading("anomaly");
        validPayload.setSeverity("CRITICAL");
        validPayload.setResidentEmail("resident@example.com");
        validPayload.setLocation("Kitchen");
        validPayload.setResidentPhoneNumber("+1234567890");
    }

    @Test
    public void testHandleIotAlert_Success() {
        Complaint savedComplaint = new Complaint();
        savedComplaint.setId(10L);
        savedComplaint.setPriority("CRITICAL");

        when(userRepository.findByEmail("resident@example.com")).thenReturn(Optional.of(testUser));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(savedComplaint);
        when(twilioService.sendSms(eq("+1234567890"), anyString())).thenReturn(null); // twilio mock return
        when(twilioService.makeCall(eq("+1234567890"), anyString())).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = iotAlertController.handleIotAlert(validPayload);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(10L, response.getBody().get("complaintId"));
        assertEquals("CRITICAL", response.getBody().get("priority"));

        verify(complaintRepository, times(1)).save(any(Complaint.class));
        verify(twilioService, times(1)).sendSms(eq("+1234567890"), anyString());
        verify(twilioService, times(1)).makeCall(eq("+1234567890"), anyString());
    }

    @Test
    public void testHandleIotAlert_MissingParameters() {
        IotAlertController.IotAlertPayload invalidPayload = new IotAlertController.IotAlertPayload();
        invalidPayload.setSensorId("leak-001");
        // missing sensorType

        ResponseEntity<Map<String, Object>> response = iotAlertController.handleIotAlert(invalidPayload);

        assertNotNull(response);
        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().containsKey("error"));
        verify(complaintRepository, never()).save(any(Complaint.class));
    }

    @Test
    public void testHandleIotAlert_FallbackPhone() {
        validPayload.setResidentPhoneNumber(null); // force fallback
        Complaint savedComplaint = new Complaint();
        savedComplaint.setId(11L);
        savedComplaint.setPriority("CRITICAL");

        when(userRepository.findByEmail("resident@example.com")).thenReturn(Optional.of(testUser));
        when(complaintRepository.save(any(Complaint.class))).thenReturn(savedComplaint);

        ResponseEntity<Map<String, Object>> response = iotAlertController.handleIotAlert(validPayload);

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        verify(twilioService, times(1)).sendSms(eq("+1234567890"), anyString()); // resolved from user profile
    }
}
