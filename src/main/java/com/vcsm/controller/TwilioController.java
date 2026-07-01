package com.vcsm.controller;

import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.vcsm.model.Complaint;
import com.vcsm.model.Event;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.EventRepository;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.TwilioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/twilio")
public class TwilioController {

    private static final Logger log = LoggerFactory.getLogger(TwilioController.class);

    @Autowired
    private TwilioService twilioService;

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    // Store call sessions (in production, use Redis or database)
    private final Map<String, Map<String, Object>> callSessions = new ConcurrentHashMap<>();

    /**
     * Initiate a call
     */
    @PostMapping("/call")
    public ResponseEntity<Map<String, Object>> initiateCall(@Valid @RequestBody Map<String, String> request) {
        String toNumber = request.get("toNumber");
        String userId = request.get("userId");

        if (toNumber == null || toNumber.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }

        Call call = twilioService.makeCall(toNumber, userId);
        if (call == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to initiate call"));
        }

        // Store session
        Map<String, Object> session = new HashMap<>();
        session.put("userId", userId);
        session.put("callSid", call.getSid());
        session.put("status", "IN_PROGRESS");
        callSessions.put(call.getSid(), session);

        return ResponseEntity.ok(Map.of(
            "success", true,
            "callSid", call.getSid(),
            "message", "Call initiated successfully"
        ));
    }

    /**
     * Webhook handler for Twilio
     */
    @PostMapping(value = "/webhook", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleWebhook(
            @RequestParam(required = false) String callerId,
            @RequestParam(required = false) String callSid) {
        
        if (callerId != null) {
            Map<String, Object> session = new HashMap<>();
            session.put("callerId", callerId);
            session.put("callSid", callSid);
            callSessions.put(callSid, session);
        }

        // Generate voice menu
        String xml = twilioService.generateVoiceMenuXml(
            "Welcome to the Virtual Customer Success Manager. How can I help you?",
            null
        );
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(xml);
    }

    /**
     * Menu handler
     */
    @PostMapping(value = "/menu", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleMenu(@RequestParam String Digits, @RequestParam String CallSid) {
        Map<String, Object> session = callSessions.get(CallSid);
        String xml;

        switch (Digits) {
            case "1":
                xml = twilioService.getComplaintTwiML();
                break;
            case "2":
                // Get complaint status
                String userId = session != null ? (String) session.get("userId") : null;
                String status = "No complaints found";
                if (userId != null) {
                    List<Complaint> complaints = complaintRepository.findByResidentUsernameOrderByCreatedAtDesc(userId);
                    if (!complaints.isEmpty()) {
                        status = "You have " + complaints.size() + " complaints. Latest status: " + complaints.get(0).getStatus();
                    }
                }
                xml = twilioService.getStatusTwiML(status);
                break;
            case "3":
                xml = twilioService.getEventTwiML();
                break;
            case "0":
                xml = getAgentTransferTwiML();
                break;
            default:
                xml = twilioService.getStatusTwiML("Invalid option selected. Please try again.");
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(xml);
    }

    /**
     * Record handler for complaint recording
     */
    @PostMapping(value = "/record", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleRecord(
            @RequestParam(required = false) String RecordingUrl,
            @RequestParam(required = false) String CallSid) {
        
        // In production, process the recording using speech-to-text
        log.info("📹 Recording URL: " + RecordingUrl);
        
        // Acknowledge receipt
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Response>" +
                "<Say voice=\"alice\">Thank you for your complaint. A support agent will review it shortly.</Say>" +
                "<Say voice=\"alice\">Goodbye.</Say>" +
                "<Hangup/>" +
                "</Response>";
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(xml);
    }

    /**
     * Event registration handler
     */
    @PostMapping(value = "/event-register", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleEventRegister(
            @RequestParam String Digits,
            @RequestParam String CallSid) {
        
        Long eventId = null;
        try {
            eventId = Long.parseLong(Digits);
        } catch (NumberFormatException e) {
            // Invalid ID
        }
        
        String xml;
        if (eventId != null) {
            Optional<Event> eventOpt = eventRepository.findById(eventId);
            if (eventOpt.isPresent()) {
                Event event = eventOpt.get();
                xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<Response>" +
                        "<Say voice=\"alice\">You have successfully registered for " + event.getName() + ".</Say>" +
                        "<Say voice=\"alice\">Goodbye.</Say>" +
                        "<Hangup/>" +
                        "</Response>";
            } else {
                xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<Response>" +
                        "<Say voice=\"alice\">Event not found. Please try again.</Say>" +
                        "<Redirect>/api/twilio/webhook</Redirect>" +
                        "</Response>";
            }
        } else {
            xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                    "<Response>" +
                    "<Say voice=\"alice\">Invalid event ID. Please try again.</Say>" +
                    "<Redirect>/api/twilio/webhook</Redirect>" +
                    "</Response>";
        }

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(xml);
    }

    /**
     * Menu return handler
     */
    @PostMapping(value = "/menu-return", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> handleMenuReturn(@RequestParam String Digits) {
        if ("1".equals(Digits)) {
            return handleWebhook(null, null);
        } else if ("0".equals(Digits)) {
            String xml = getAgentTransferTwiML();
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
        }
        
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Response>" +
                "<Say voice=\"alice\">Invalid option. Returning to main menu.</Say>" +
                "<Redirect>/api/twilio/webhook</Redirect>" +
                "</Response>";
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_XML)
            .body(xml);
    }

    /**
     * Send SMS
     */
    @PostMapping("/sms")
    public ResponseEntity<Map<String, Object>> sendSms(@Valid @RequestBody Map<String, String> request) {
        String toNumber = request.get("toNumber");
        String message = request.get("message");

        if (toNumber == null || toNumber.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Phone number is required"));
        }
        if (message == null || message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message is required"));
        }

        Message sms = twilioService.sendSms(toNumber, message);
        if (sms == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to send SMS"));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "smsSid", sms.getSid(),
            "message", "SMS sent successfully"
        ));
    }

    /**
     * Transfer to agent (mock)
     */
    private String getAgentTransferTwiML() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<Response>" +
                "<Say voice=\"alice\">Connecting you to a support agent. Please hold.</Say>" +
                "<Dial>" +
                "<Number>+1234567890</Number>" +
                "</Dial>" +
                "</Response>";
    }
}