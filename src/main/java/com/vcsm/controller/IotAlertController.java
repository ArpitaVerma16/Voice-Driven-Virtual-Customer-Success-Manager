package com.vcsm.controller;

import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.TwilioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/iot")
@CrossOrigin(origins = "*")
public class IotAlertController {

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TwilioService twilioService;

    @PostMapping("/alert")
    public ResponseEntity<Map<String, Object>> handleIotAlert(@Valid @RequestBody IotAlertPayload payload) {
        if (payload.getSensorId() == null || payload.getSensorType() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Sensor ID and type are required"));
        }

        // 1. Look up user
        Optional<User> userOpt = Optional.empty();
        if (payload.getResidentEmail() != null && !payload.getResidentEmail().isEmpty()) {
            userOpt = userRepository.findByEmail(payload.getResidentEmail());
        }
        User user = userOpt.orElse(null);

        // 2. Create high priority complaint ticket
        Complaint complaint = new Complaint();
        complaint.setResidentName(user != null ? user.getName() : "IoT Sensor System");
        complaint.setResidentUsername(payload.getResidentEmail() != null ? payload.getResidentEmail() : "system@vcsm.com");
        complaint.setContactEmail(payload.getResidentEmail() != null ? payload.getResidentEmail() : "system@vcsm.com");
        complaint.setApartmentNumber(payload.getLocation());
        
        // Map sensor type to category
        Complaint.ComplaintCategory category = Complaint.ComplaintCategory.UTILITIES;
        if ("GAS_LEAK".equalsIgnoreCase(payload.getSensorType())) {
            category = Complaint.ComplaintCategory.SECURITY;
        } else if ("MAINTENANCE".equalsIgnoreCase(payload.getSensorType())) {
            category = Complaint.ComplaintCategory.MAINTENANCE;
        }
        complaint.setCategory(category);

        String desc = String.format("IoT Emergency Alert: %s sensor %s at %s detected an anomaly! Reading: %s. Severity: %s.",
                payload.getSensorType(), payload.getSensorId(), payload.getLocation() != null ? payload.getLocation() : "Unknown location",
                payload.getReading(), payload.getSeverity());
        complaint.setDescription(desc);
        complaint.setPriority("CRITICAL");
        complaint.setAutoAssigned(true);
        complaint.setUser(user);

        Complaint saved = complaintRepository.save(complaint);

        // 3. Trigger Twilio notifications
        String targetPhone = payload.getResidentPhoneNumber();
        if (targetPhone == null || targetPhone.isEmpty()) {
            if (user != null) {
                targetPhone = user.getPhoneNumber() != null ? user.getPhoneNumber() : user.getPhone();
            }
        }
        if (targetPhone == null || targetPhone.isEmpty()) {
            targetPhone = "+15555555555"; // Default fallback
        }

        String smsMessage = String.format("URGENT IoT Alert: %s sensor %s at %s detected an anomaly! A CRITICAL complaint ticket #%d has been filed. Please check immediately.",
                payload.getSensorType(), payload.getSensorId(), payload.getLocation(), saved.getId());

        boolean smsTriggered = false;
        boolean callTriggered = false;

        try {
            var sms = twilioService.sendSms(targetPhone, smsMessage);
            if (sms != null) {
                smsTriggered = true;
            }
        } catch (RuntimeException e) {
            // Ignore
        }

        try {
            var call = twilioService.makeCall(targetPhone, "IoT Alert Service");
            if (call != null) {
                callTriggered = true;
            }
        } catch (RuntimeException e) {
            // Ignore
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("complaintId", saved.getId());
        response.put("priority", saved.getPriority());
        response.put("smsTriggered", smsTriggered);
        response.put("callTriggered", callTriggered);
        response.put("message", "Emergency complaint ticket filed and Twilio notifications triggered.");

        return ResponseEntity.ok(response);
    }

    public static class IotAlertPayload {
        private String sensorId;
        private String sensorType;
        private String reading;
        private String severity;
        private String residentEmail;
        private String location;
        private String residentPhoneNumber;

        public String getSensorId() { return sensorId; }
        public void setSensorId(String sensorId) { this.sensorId = sensorId; }

        public String getSensorType() { return sensorType; }
        public void setSensorType(String sensorType) { this.sensorType = sensorType; }

        public String getReading() { return reading; }
        public void setReading(String reading) { this.reading = reading; }

        public String getSeverity() { return severity; }
        public void setSeverity(String severity) { this.severity = severity; }

        public String getResidentEmail() { return residentEmail; }
        public void setResidentEmail(String residentEmail) { this.residentEmail = residentEmail; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getResidentPhoneNumber() { return residentPhoneNumber; }
        public void setResidentPhoneNumber(String residentPhoneNumber) { this.residentPhoneNumber = residentPhoneNumber; }
    }
}
