package com.vcsm.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Service
public class TwilioService {

    private static final Logger log = LoggerFactory.getLogger(TwilioService.class);

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;

    @Value("${twilio.webhook.url}")
    private String webhookUrl;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("✅ Twilio initialized with SID: " + accountSid);
    }

    /**
     * Make an outbound call
     */
    public Call makeCall(String toPhoneNumber, String callerId) {
        try {
            PhoneNumber to = new PhoneNumber(toPhoneNumber);
            PhoneNumber from = new PhoneNumber(twilioPhoneNumber);
            
            // Build the webhook URL for call handling
            String callUrl = webhookUrl + "?callerId=" + (callerId != null ? callerId : "unknown");
            
            Call call = Call.creator(to, from, URI.create(callUrl))
                .create();
            
            log.info("📞 Call initiated: " + call.getSid());
            return call;
            
        } catch (Exception e) {
            log.error("❌ Failed to make call: " + e.getMessage());
            log.error("Failed to make call to {}: {}", toPhoneNumber, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Send SMS notification
     */
    public Message sendSms(String toPhoneNumber, String message) {
        try {
            PhoneNumber to = new PhoneNumber(toPhoneNumber);
            PhoneNumber from = new PhoneNumber(twilioPhoneNumber);
            
            Message sms = Message.creator(to, from, message)
                .create();
            
            log.info("📱 SMS sent: " + sms.getSid());
            return sms;
            
        } catch (Exception e) {
            log.error("❌ Failed to send SMS: " + e.getMessage());
            log.error("Failed to send SMS to {}: {}", toPhoneNumber, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Generate TwiML for voice menu
     */
    public String generateVoiceMenuXml(String message, Map<String, String> options) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<Response>");
        
        // Greeting
        xml.append("<Say voice=\"alice\">");
        xml.append(message);
        xml.append("</Say>");
        
        // Menu options
        xml.append("<Gather numDigits=\"1\" action=\"/api/twilio/menu\" method=\"POST\">");
        xml.append("<Say voice=\"alice\">");
        xml.append("Press 1 for complaint filing. Press 2 for complaint status. Press 3 for event registration. Press 0 to speak to an agent.");
        xml.append("</Say>");
        xml.append("</Gather>");
        
        // If no input
        xml.append("<Say voice=\"alice\">");
        xml.append("We didn't receive any input. Please try again.");
        xml.append("</Say>");
        xml.append("<Redirect>/api/twilio/webhook</Redirect>");
        
        xml.append("</Response>");
        return xml.toString();
    }

    /**
     * Generate TwiML for complaint filing
     */
    public String getComplaintTwiML() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<Response>");
        xml.append("<Say voice=\"alice\">");
        xml.append("Please speak your complaint after the beep. Press the star key when you are done.");
        xml.append("</Say>");
        xml.append("<Record action=\"/api/twilio/record\" method=\"POST\" maxLength=\"120\" finishOnKey=\"*\" />");
        xml.append("<Say voice=\"alice\">");
        xml.append("Your complaint has been recorded. Thank you.");
        xml.append("</Say>");
        xml.append("</Response>");
        return xml.toString();
    }

    /**
     * Generate TwiML for complaint status
     */
    public String getStatusTwiML(String status) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<Response>");
        xml.append("<Say voice=\"alice\">");
        
        if (status == null || status.isEmpty()) {
            xml.append("No complaints found for your account.");
        } else {
            xml.append("Your complaint status is: " + status);
        }
        
        xml.append("</Say>");
        xml.append("<Say voice=\"alice\">");
        xml.append("Press 1 to go back to the main menu, or press 0 to speak to an agent.");
        xml.append("</Say>");
        xml.append("<Gather numDigits=\"1\" action=\"/api/twilio/menu-return\" method=\"POST\">");
        xml.append("</Gather>");
        xml.append("</Response>");
        return xml.toString();
    }

    /**
     * Generate TwiML for event registration
     */
    public String getEventTwiML() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        xml.append("<Response>");
        xml.append("<Say voice=\"alice\">");
        xml.append("Please enter your event ID using the keypad.");
        xml.append("</Say>");
        xml.append("<Gather numDigits=\"5\" action=\"/api/twilio/event-register\" method=\"POST\">");
        xml.append("</Gather>");
        xml.append("<Say voice=\"alice\">");
        xml.append("No input received. Please try again.");
        xml.append("</Say>");
        xml.append("<Redirect>/api/twilio/webhook</Redirect>");
        xml.append("</Response>");
        return xml.toString();
    }
}