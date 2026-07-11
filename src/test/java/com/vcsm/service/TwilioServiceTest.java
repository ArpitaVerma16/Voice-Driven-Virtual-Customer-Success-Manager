package com.vcsm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TwilioServiceTest {

    private TwilioService twilioService;

    @BeforeEach
    void setUp() {
        twilioService = new TwilioService();
        ReflectionTestUtils.setField(twilioService, "accountSid", "test_sid");
        ReflectionTestUtils.setField(twilioService, "authToken", "test_token");
        ReflectionTestUtils.setField(twilioService, "twilioPhoneNumber", "+1234567890");
        ReflectionTestUtils.setField(twilioService, "webhookUrl", "https://example.com/api/twilio/webhook");
    }

    @Test
    void generateVoiceMenuXml_shouldContainSayAndGather() {
        String xml = twilioService.generateVoiceMenuXml("Welcome!", null);
        assertTrue(xml.contains("<Response>"));
        assertTrue(xml.contains("<Say voice=\"alice\">"));
        assertTrue(xml.contains("<Gather numDigits=\"1\""));
        assertTrue(xml.contains("Welcome!"));
    }

    @Test
    void generateVoiceMenuXml_shouldIncludeRedirect() {
        String xml = twilioService.generateVoiceMenuXml("Test", null);
        assertTrue(xml.contains("<Redirect>/api/twilio/webhook</Redirect>"));
    }

    @Test
    void getComplaintTwiML_shouldContainRecordTag() {
        String xml = twilioService.getComplaintTwiML();
        assertTrue(xml.contains("<Record"));
        assertTrue(xml.contains("finishOnKey=\"*\""));
        assertTrue(xml.contains("maxLength=\"120\""));
    }

    @Test
    void getComplaintTwiML_shouldIncludeSpeakPrompt() {
        String xml = twilioService.getComplaintTwiML();
        assertTrue(xml.contains("Please speak your complaint after the beep"));
        assertTrue(xml.contains("Your complaint has been recorded"));
    }

    @Test
    void getStatusTwiML_shouldShowStatus() {
        String xml = twilioService.getStatusTwiML("IN_PROGRESS");
        assertTrue(xml.contains("Your complaint status is: IN_PROGRESS"));
    }

    @Test
    void getStatusTwiML_shouldHandleNullStatus() {
        String xml = twilioService.getStatusTwiML(null);
        assertTrue(xml.contains("No complaints found for your account."));
    }

    @Test
    void getStatusTwiML_shouldHandleEmptyStatus() {
        String xml = twilioService.getStatusTwiML("");
        assertTrue(xml.contains("No complaints found for your account."));
    }

    @Test
    void getStatusTwiML_shouldIncludeGatherForMenuReturn() {
        String xml = twilioService.getStatusTwiML("RESOLVED");
        assertTrue(xml.contains("<Gather numDigits=\"1\" action=\"/api/twilio/menu-return\""));
        assertTrue(xml.contains("Press 1 to go back to the main menu"));
    }

    @Test
    void getEventTwiML_shouldIncludeGatherForEventId() {
        String xml = twilioService.getEventTwiML();
        assertTrue(xml.contains("<Gather numDigits=\"5\""));
        assertTrue(xml.contains("action=\"/api/twilio/event-register\""));
    }

    @Test
    void getEventTwiML_shouldIncludeRedirectForNoInput() {
        String xml = twilioService.getEventTwiML();
        assertTrue(xml.contains("<Redirect>/api/twilio/webhook</Redirect>"));
    }

    @Test
    void xmlShouldBeWellFormed() {
        String xml = twilioService.getComplaintTwiML();
        int openCount = xml.split("<Response>", -1).length - 1;
        int closeCount = xml.split("</Response>", -1).length - 1;
        assertEquals(openCount, closeCount);
    }

    @Test
    void statusTwiML_shouldBeWellFormed() {
        String xml = twilioService.getStatusTwiML("OPEN");
        int openCount = xml.split("<Response>", -1).length - 1;
        int closeCount = xml.split("</Response>", -1).length - 1;
        assertEquals(openCount, closeCount);
    }
}
