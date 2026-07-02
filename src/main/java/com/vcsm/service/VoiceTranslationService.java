package com.vcsm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Service
@lombok.RequiredArgsConstructor
public class VoiceTranslationService {

    private static final Logger log = LoggerFactory.getLogger(VoiceTranslationService.class);

    private final LanguageDetector languageDetector;

    @Value("${google.translate.api.key:}")
    private String apiKey;

    @Value("${google.speech.api.key:}")
    private String speechApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Translate text from source language to target language
     */
    @CircuitBreaker(name = "translationService", fallbackMethod = "translateTextFallback")
    public String translateText(String text, String sourceLang, String targetLang) {
        if (text == null || text.isEmpty()) {
            return org.springframework.http.ResponseEntity.ok("");
        }
        
        if (sourceLang.equals(targetLang)) {
            return text;
        }

        // If no API key, use fallback translation
        if (apiKey == null || apiKey.isEmpty()) {
            return getFallbackTranslation(text, sourceLang, targetLang);
        }

        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String urlStr = "https://translation.googleapis.com/language/translate/v2?" +
                    "key=" + apiKey +
                    "&q=" + encodedText +
                    "&source=" + sourceLang +
                    "&target=" + targetLang;
            
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                
                // Parse response
                Map<String, Object> json = objectMapper.readValue(
                    response.toString(),
                    new TypeReference<Map<String, Object>>() {}
                );
                
                Map<String, Object> data = (Map<String, Object>) json.get("data");
                if (data != null) {
                    Object translations = data.get("translations");
                    if (translations instanceof java.util.List) {
                        java.util.List<Map<String, Object>> list =
                            (java.util.List<Map<String, Object>>) translations;
                        if (!list.isEmpty()) {
                            return (String) list.get(0).get("translatedText");
                        }
                    }
                }
                return text;
            } finally {
                conn.disconnect();
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Translation API call failed", e);
        }
    }

    /**
     * Auto-detect language and translate to target
     */
    public String autoTranslate(String text, String targetLang) {
        String sourceLang = languageDetector.detectLanguage(text);
        return translateText(text, sourceLang, targetLang);
    }

    public String translateTextFallback(String text, String sourceLang, String targetLang, Throwable t) {
        log.warn("Circuit breaker triggered for translation service: {}", t.getMessage());
        return org.springframework.http.ResponseEntity.ok("Service temporarily unavailable, please try again.");
    }

    /**
     * Mock translation for Hindi/English/Spanish (fallback when API key is missing)
     */
    private String getFallbackTranslation(String text, String sourceLang, String targetLang) {
        // If translation is Hindi -> English or vice versa
        if (sourceLang.equals("hi") && targetLang.equals("en")) {
            return getHindiToEnglishMock(text);
        } else if (sourceLang.equals("en") && targetLang.equals("hi")) {
            return getEnglishToHindiMock(text);
        } else if (sourceLang.equals("es") && targetLang.equals("en")) {
            return getSpanishToEnglishMock(text);
        } else if (sourceLang.equals("en") && targetLang.equals("es")) {
            return getEnglishToSpanishMock(text);
        }
        return text + " [translated to " + targetLang + "]";
    }

    private String getHindiToEnglishMock(String text) {
        // Simple mock translations for common phrases
        String lower = text.toLowerCase();
        if (lower.contains("शिकायत") || lower.contains("shikayat")) {
            return org.springframework.http.ResponseEntity.ok("Complaint filed successfully");
        } else if (lower.contains("नमस्ते") || lower.contains("namaste")) {
            return org.springframework.http.ResponseEntity.ok("Hello! How can I help you?");
        } else if (lower.contains("धन्यवाद") || lower.contains("dhanyavaad")) {
            return org.springframework.http.ResponseEntity.ok("You're welcome!");
        } else if (lower.contains("मदद") || lower.contains("madad")) {
            return org.springframework.http.ResponseEntity.ok("I'm here to help you");
        } else if (lower.contains("स्टेटस") || lower.contains("status")) {
            return org.springframework.http.ResponseEntity.ok("Your complaint status is: In Progress");
        } else if (lower.contains("इवेंट") || lower.contains("event")) {
            return org.springframework.http.ResponseEntity.ok("Events are available on the Events page");
        } else {
            return org.springframework.http.ResponseEntity.ok("I understand your query. Our team will look into it.");
        }
    }

    private String getEnglishToHindiMock(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("complaint")) {
            return org.springframework.http.ResponseEntity.ok("शिकायत सफलतापूर्वक दर्ज की गई");
        } else if (lower.contains("hello") || lower.contains("hi")) {
            return org.springframework.http.ResponseEntity.ok("नमस्ते! मैं आपकी कैसे मदद कर सकता हूँ?");
        } else if (lower.contains("thank")) {
            return org.springframework.http.ResponseEntity.ok("आपका स्वागत है!");
        } else if (lower.contains("help")) {
            return org.springframework.http.ResponseEntity.ok("मैं आपकी मदद के लिए यहाँ हूँ");
        } else if (lower.contains("status")) {
            return org.springframework.http.ResponseEntity.ok("आपकी शिकायत का स्टेटस: प्रगति पर है");
        } else if (lower.contains("event")) {
            return org.springframework.http.ResponseEntity.ok("इवेंट पेज पर इवेंट उपलब्ध हैं");
        } else {
            return org.springframework.http.ResponseEntity.ok("मैं आपका प्रश्न समझ गया। हमारी टीम इस पर ध्यान देगी।");
        }
    }

    private String getSpanishToEnglishMock(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("problema") || lower.contains("queja")) {
            return org.springframework.http.ResponseEntity.ok("Complaint filed successfully");
        } else if (lower.contains("hola")) {
            return org.springframework.http.ResponseEntity.ok("Hello! How can I help you?");
        } else if (lower.contains("gracias")) {
            return org.springframework.http.ResponseEntity.ok("You're welcome!");
        } else if (lower.contains("ayuda")) {
            return org.springframework.http.ResponseEntity.ok("I'm here to help you");
        } else if (lower.contains("estado")) {
            return org.springframework.http.ResponseEntity.ok("Your complaint status is: In Progress");
        } else if (lower.contains("evento")) {
            return org.springframework.http.ResponseEntity.ok("Events are available on the Events page");
        } else {
            return org.springframework.http.ResponseEntity.ok("I understand your query. Our team will look into it.");
        }
    }

    private String getEnglishToSpanishMock(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("complaint")) {
            return org.springframework.http.ResponseEntity.ok("Queja registrada con éxito");
        } else if (lower.contains("hello") || lower.contains("hi")) {
            return org.springframework.http.ResponseEntity.ok("¡Hola! ¿Cómo puedo ayudarte?");
        } else if (lower.contains("thank")) {
            return org.springframework.http.ResponseEntity.ok("¡De nada!");
        } else if (lower.contains("help")) {
            return org.springframework.http.ResponseEntity.ok("Estoy aquí para ayudarte");
        } else if (lower.contains("status")) {
            return org.springframework.http.ResponseEntity.ok("El estado de tu queja es: En progreso");
        } else if (lower.contains("event")) {
            return org.springframework.http.ResponseEntity.ok("Los eventos están disponibles en la página de eventos");
        } else {
            return org.springframework.http.ResponseEntity.ok("Entiendo tu consulta. Nuestro equipo la revisará.");
        }
    }

    /**
     * Get supported languages for the UI
     */
    public Map<String, String> getSupportedLanguages() {
        return languageDetector.getSupportedLanguages();
    }
}

