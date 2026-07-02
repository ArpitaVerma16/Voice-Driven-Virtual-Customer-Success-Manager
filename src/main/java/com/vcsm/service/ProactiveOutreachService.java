package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProactiveOutreachService {

    private static final Logger log = LoggerFactory.getLogger(ProactiveOutreachService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBehaviorMonitor behaviorMonitor;

    @Autowired
    private EmailService emailService;

    /**
     * Send proactive outreach to at-risk users
     */
    public OutreachResult sendProactiveOutreach(Long userId, String channel) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return new OutreachResult(false, "User not found", null);
        }

        UserBehaviorMonitor.BehaviorAnalysis analysis = behaviorMonitor.analyzeUserBehavior(user);

        if (analysis.getRiskScore() < 20) {
            return new OutreachResult(false, "User not at risk", null);
        }

        String message = generateProactiveMessage(analysis);
        String subject = getSubjectForRisk(analysis.getRiskScore());

        boolean success = false;
        String sentVia = "";

        if ("email".equalsIgnoreCase(channel)) {
            success = sendEmail(user, subject, message);
            sentVia = "Email";
        } else if ("sms".equalsIgnoreCase(channel)) {
            success = sendSms(user, message);
            sentVia = "SMS";
        } else {
            // Default to email
            success = sendEmail(user, subject, message);
            sentVia = "Email";
        }

        return new OutreachResult(success, sentVia, message);
    }

    private String generateProactiveMessage(UserBehaviorMonitor.BehaviorAnalysis analysis) {
        StringBuilder message = new StringBuilder();

        message.append("Hello " + analysis.getUserName() + ",\n\n");
        message.append("We noticed you've been interacting with our support system.\n\n");

        if (analysis.getRiskScore() >= 70) {
            message.append("We understand you're facing some challenges. We want to help you directly.\n");
            message.append("A senior support representative will reach out to you shortly.\n\n");
        } else if (analysis.getRiskScore() >= 40) {
            message.append("We see you have some open issues. We'd like to make sure everything is on track.\n");
            message.append("Please reply to this message or call us for immediate assistance.\n\n");
        } else {
            message.append("We value your feedback and want to ensure you're having a good experience.\n");
            message.append("Let us know if there's anything we can help with.\n\n");
        }

        message.append("Your support team is here for you.\n");
        message.append("Best regards,\n");
        message.append("VCSM Support Team");

        return message.toString();
    }

    private String getSubjectForRisk(int riskScore) {
        if (riskScore >= 70) {
            return org.springframework.http.ResponseEntity.ok("🔴 We're Here to Help - Urgent Support Needed");
        } else if (riskScore >= 40) {
            return org.springframework.http.ResponseEntity.ok("🟡 We Noticed Your Recent Interactions - Let's Help");
        } else {
            return org.springframework.http.ResponseEntity.ok("ℹ️ Check-in: How Are Things Going?");
        }
    }

    private boolean sendEmail(User user, String subject, String message) {
        try {
            emailService.sendSimpleEmail(user.getEmail(), subject, message);
            return true;
        } catch (Exception e) {
            log.error("Failed to send email: " + e.getMessage());
            log.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage(), e);
            return false;
        }
    }

    private boolean sendSms(User user, String message) {
        // SMS implementation placeholder
        log.info("📱 SMS to " + user.getPhone() + ": " + message);
        return true;
    }

    public static class OutreachResult {
        private final boolean success;
        private final String message;
        private final String details;

        public OutreachResult(boolean success, String message, String details) {
            this.success = success;
            this.message = message;
            this.details = details;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getDetails() { return details; }
    }
}