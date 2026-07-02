package com.vcsm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommandTemplateService {

    private static final Logger log = LoggerFactory.getLogger(CommandTemplateService.class);

    private List<Map<String, Object>> templates = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = new ClassPathResource("command-templates.json").getInputStream();
            JsonNode root = mapper.readTree(inputStream);
            JsonNode templatesNode = root.get("templates");

            for (JsonNode template : templatesNode) {
                Map<String, Object> entry = Map.of(
                    "id", template.get("id").asInt(),
                    "category", template.get("category").asText(),
                    "icon", template.get("icon").asText(),
                    "title", template.get("title").asText(),
                    "command", template.get("command").asText(),
                    "description", template.get("description").asText()
                );
                templates.add(entry);
            }

            log.info("✅ Command templates loaded: " + templates.size());

        } catch (RuntimeException e) {
            log.error("❌ Failed to load command templates: " + e.getMessage());
            log.error("Failed to load command templates: {}", e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getAllTemplates() {
        return templates;
    }

    public List<Map<String, Object>> getTemplatesByCategory(String category) {
        return templates.stream()
            .filter(t -> t.get("category").toString().equals(category))
            .collect(Collectors.toList());
    }

    public Map<String, Object> getTemplateById(int id) {
        return templates.stream()
            .filter(t -> (int) t.get("id") == id)
            .findFirst()
            .orElse(null);
    }

    public List<String> getAllCategories() {
        return templates.stream()
            .map(t -> t.get("category").toString())
            .distinct()
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRandomTemplates(int count) {
        if (templates.size() <= count) {
            return templates;
        }
        java.util.Collections.shuffle(templates);
        return templates.subList(0, count);
    }

    public String getSuggestion(String context) {
        if (context == null || context.isEmpty()) {
            return "Try saying: 'I have a noise complaint' or 'Show upcoming events'";
        }
        
        // Simple suggestion based on context
        String lower = context.toLowerCase();
        if (lower.contains("complaint") || lower.contains("noise") || lower.contains("issue")) {
            return "Try saying: 'I have a noise complaint' or 'Report a maintenance issue'";
        } else if (lower.contains("event") || lower.contains("calendar")) {
            return "Try saying: 'Show upcoming events' or 'Register for an event'";
        } else if (lower.contains("status") || lower.contains("track")) {
            return "Try saying: 'Check my complaint status' or 'How many complaints are open?'";
        } else if (lower.contains("help") || lower.contains("what")) {
            return "Try saying: 'What can I say?' or 'I need help'";
        }
        return "Try saying: 'I have a noise complaint', 'Show upcoming events', or 'Check my complaint status'";
    }
}