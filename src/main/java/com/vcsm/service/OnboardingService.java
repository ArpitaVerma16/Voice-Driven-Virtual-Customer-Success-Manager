package com.vcsm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class OnboardingService {

    private static final Logger log = LoggerFactory.getLogger(OnboardingService.class);

    private List<Map<String, Object>> tutorialSteps = new ArrayList<>();

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            InputStream inputStream = new ClassPathResource("tutorial-steps.json").getInputStream();
            JsonNode root = mapper.readTree(inputStream);
            JsonNode steps = root.get("steps");
            
            for (JsonNode step : steps) {
                Map<String, Object> stepMap = Map.of(
                    "id", step.get("id").asInt(),
                    "title", step.get("title").asText(),
                    "message", step.get("message").asText(),
                    "highlight", step.get("highlight") != null ? step.get("highlight").asText() : null,
                    "voiceText", step.get("voiceText").asText()
                );
                tutorialSteps.add(stepMap);
            }
            
            log.info("✅ Tutorial steps loaded: " + tutorialSteps.size() + " steps");
            
        } catch (SpecificException e) {
            log.error("❌ Failed to load tutorial steps: " + e.getMessage());
            log.error("Failed to load tutorial steps: {}", e.getMessage(), e);
        }
    }

    public List<Map<String, Object>> getTutorialSteps() {
        return tutorialSteps;
    }

    public Map<String, Object> getStep(int id) {
        for (Map<String, Object> step : tutorialSteps) {
            if ((int) step.get("id") == id) {
                return step;
            }
        }
        return null;
    }
}