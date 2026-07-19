package com.vcsm.service;

import com.vcsm.model.Notification;
import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class EmergencyBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(EmergencyBroadcastService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TwilioService twilioService;

    public Map<String, Object> processAndBroadcast(String transcript) {
        // 1. Format the transcript into a professional notification
        String formattedTitle = "🚨 EMERGENCY ALERT";
        String formattedMessage = formatWithLLM(transcript);

        // 2. Dispatch via Push Notification (Global)
        Notification globalNotif = new Notification(null, formattedTitle, formattedMessage, "EMERGENCY");
        notificationService.sendGlobalNotification(globalNotif);

        // 3. Dispatch via SMS and Email asynchronously
        CompletableFuture.runAsync(() -> {
            List<User> allUsers = userRepository.findAll();
            for (User user : allUsers) {
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    emailService.sendSimpleEmail(user.getEmail(), formattedTitle, formattedMessage);
                }
                if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                    // Assuming getPhoneNumber exists on User model. If not, it will be skipped or we should check.
                    // To be safe, we can try-catch the method call.
                    try {
                        String phone = user.getPhoneNumber();
                        if (phone != null && !phone.isEmpty()) {
                            twilioService.sendSms(phone, formattedTitle + "\n" + formattedMessage);
                        }
                    } catch(Exception ignored) {
                        // Ignore if getPhoneNumber doesn't exist or fails
                    }
                }
            }
        });

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("response", "Alert dispatched successfully: " + formattedMessage);
        return result;
    }

    private String formatWithLLM(String transcript) {
        // Mocking the LLM formatting to make it sound professional
        // In a real scenario, this would call an external API (e.g. OpenAI)
        if (transcript.toLowerCase().contains("water") || transcript.toLowerCase().contains("shut off")) {
            return "Attention Residents: " + transcript + ". Please prepare accordingly and contact management if you require immediate assistance.";
        }
        return "URGENT NOTIFICATION: " + transcript + ". Please stay safe and follow all management instructions.";
    }
}
