package com.vcsm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);

    private List<Map<String, Object>> faqs = new ArrayList<>();
    private Map<String, List<Integer>> keywordIndex = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = new ClassPathResource("faq-data.json").getInputStream();
            JsonNode root = mapper.readTree(inputStream);
            JsonNode faqsNode = root.get("faqs");

            int index = 0;
            for (JsonNode faq : faqsNode) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("id", faq.get("id").asInt());
                entry.put("question", faq.get("question").asText());
                entry.put("answer", faq.get("answer").asText());

                List<String> keywords = new ArrayList<>();
                JsonNode keywordsNode = faq.get("keywords");
                for (JsonNode kw : keywordsNode) {
                    String keyword = kw.asText().toLowerCase();
                    keywords.add(keyword);
                    keywordIndex.computeIfAbsent(keyword, k -> new ArrayList<>()).add(index);
                }
                entry.put("keywords", keywords);

                faqs.add(entry);
                index++;
            }

            log.info("✅ Chatbot loaded with " + faqs.size() + " FAQs");

        } catch (Exception e) {
            log.error("❌ Failed to load FAQs: " + e.getMessage());
        }
    }

    public String getResponse(String userMessage) {
        if (userMessage == null || userMessage.isEmpty()) {
            return "Please ask me something! I'm here to help.";
        }

        String lowerMessage = userMessage.toLowerCase().trim();

        // Check for exact FAQ match
        for (Map<String, Object> faq : faqs) {
            String question = ((String) faq.get("question")).toLowerCase();
            if (lowerMessage.contains(question) || question.contains(lowerMessage)) {
                return (String) faq.get("answer");
            }
        }

        // Check by keywords
        Set<Integer> matchedIndices = new HashSet<>();
        for (Map.Entry<String, List<Integer>> entry : keywordIndex.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                matchedIndices.addAll(entry.getValue());
            }
        }

        if (!matchedIndices.isEmpty()) {
            int bestIndex = matchedIndices.iterator().next();
            return (String) faqs.get(bestIndex).get("answer");
        }

        return getDefaultResponse();
    }

    public String getDefaultResponse() {
        return """
            I'm your VCSM Assistant! 🤖
            
            You can ask me about:
            📝 How to file a complaint
            📊 Checking complaint status
            📅 Registering for events
            🎤 Using voice commands
            🔔 Notifications
            🔐 2FA setup
            👤 Profile updates
            
            Just type your question below! 😊
            """;
    }
}