package com.vcsm.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@Service
public class WebhookNotificationService {

    private static final Logger log = Logger.getLogger(WebhookNotificationService.class.getName());
    private final RestTemplate restTemplate;

    @Value("${webhook.escalation.url:}")
    private String webhookUrl;

    public WebhookNotificationService() {
        this.restTemplate = new RestTemplate();
    }

    public void sendEscalationAlert(String sessionId, String customerId, String escalationReason) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warning("Webhook URL is not configured. Skipping escalation alert.");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> payload = new HashMap<>();
            payload.put("sessionId", sessionId != null ? sessionId : "unknown");
            payload.put("customerId", customerId != null ? customerId : "unknown");
            payload.put("escalationReason", escalationReason != null ? escalationReason : "Distressed sentiment detected");
            payload.put("alertType", "URGENT_ESCALATION");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            restTemplate.postForEntity(webhookUrl, request, String.class);
            log.info("Successfully sent escalation alert to webhook for session: " + sessionId);
        } catch (Exception e) {
            log.severe("Failed to send escalation alert to webhook: " + e.getMessage());
        }
    }
}
