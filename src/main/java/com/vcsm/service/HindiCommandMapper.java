package com.vcsm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class HindiCommandMapper {

    private static final Logger log = LoggerFactory.getLogger(HindiCommandMapper.class);
    
    private Map<String, String> commandMap = new ConcurrentHashMap<>();
    private Map<String, String> responseMap = new ConcurrentHashMap<>();
    private Map<String, String[]> keywordMap = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = new ClassPathResource("hindi_commands.json").getInputStream();
            JsonNode root = mapper.readTree(inputStream);
            
            // Load commands
            JsonNode commands = root.get("commands");
            commands.fields().forEachRemaining(entry -> {
                commandMap.put(entry.getKey(), entry.getValue().asText());
            });
            
            // Load responses
            JsonNode responses = root.get("responses");
            responses.fields().forEachRemaining(entry -> {
                responseMap.put(entry.getKey(), entry.getValue().asText());
            });
            
            // Load keywords
            JsonNode keywords = root.get("keywords");
            keywords.fields().forEachRemaining(entry -> {
                String[] words = entry.getValue().toString()
                    .replace("[", "").replace("]", "").replace("\"", "").split(",");
                keywordMap.put(entry.getKey(), words);
            });
            
            log.info("✅ Hindi commands loaded: " + commandMap.size() + " commands");
            
        } catch (RuntimeException e) {
            log.error("❌ Failed to load Hindi commands: " + e.getMessage());
            log.error("Failed to load Hindi commands: {}", e.getMessage(), e);
        }
    }
    
    public String mapCommand(String hindiText) {
        if (hindiText == null || hindiText.isEmpty()) {
            return null;
        }
        
        String lowerText = hindiText.toLowerCase();
        
        // Exact match
        if (commandMap.containsKey(hindiText)) {
            return commandMap.get(hindiText);
        }
        
        // Partial match using keywords
        for (Map.Entry<String, String[]> entry : keywordMap.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    return mapKeywordToAction(entry.getKey());
                }
            }
        }
        
        return null;
    }
    
    private String mapKeywordToAction(String keyword) {
        switch (keyword) {
            case "complaint": return org.springframework.http.ResponseEntity.ok("file_complaint");
            case "event": return org.springframework.http.ResponseEntity.ok("show_events");
            case "status": return org.springframework.http.ResponseEntity.ok("complaint_status");
            case "help": return org.springframework.http.ResponseEntity.ok("help");
            case "cancel": return org.springframework.http.ResponseEntity.ok("cancel_registration");
            default: return null;
        }
    }
    
    public String getResponse(String action, String extraData) {
        String response = responseMap.getOrDefault(action, responseMap.get("default"));
        if (extraData != null && !extraData.isEmpty()) {
            response += extraData;
        }
        return response;
    }
    
    public String getDefaultResponse() {
        return responseMap.get("default");
    }
    
    public String getThankYouResponse() {
        return responseMap.get("thank_you");
    }
}
