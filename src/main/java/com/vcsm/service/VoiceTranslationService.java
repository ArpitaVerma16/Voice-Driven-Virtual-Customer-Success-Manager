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
public class VoiceTranslationService {

    private static final Logger log = LoggerFactory.getLogger(VoiceTranslationService.class);

    @Autowired
    private LanguageDetector languageDetector;

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
            return "";
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
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            
            // Parse response
            Map<String, Object> json = objectMapper.readValue(
                response.toString(), 
                Map.class
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
        return "Service temporarily unavailable, please try again.";
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
            return "Complaint filed successfully";
        } else if (lower.contains("नमस्ते") || lower.contains("namaste")) {
            return "Hello! How can I help you?";
        } else if (lower.contains("धन्यवाद") || lower.contains("dhanyavaad")) {
            return "You're welcome!";
        } else if (lower.contains("मदद") || lower.contains("madad")) {
            return "I'm here to help you";
        } else if (lower.contains("स्टेटस") || lower.contains("status")) {
            return "Your complaint status is: In Progress";
        } else if (lower.contains("इवेंट") || lower.contains("event")) {
            return "Events are available on the Events page";
        } else {
            return "I understand your query. Our team will look into it.";
        }
    }

    private String getEnglishToHindiMock(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("complaint")) {
            return "शिकायत सफलतापूर्वक दर्ज की गई";
        } else if (lower.contains("hello") || lower.contains("hi")) {
            return "नमस्ते! मैं आपकी कैसे मदद कर सकता हूँ?";
        } else if (lower.contains("thank")) {
            return "आपका स्वागत है!";
        } else if (lower.contains("help")) {
            return "मैं आपकी मदद के लिए यहाँ हूँ";
        } else if (lower.contains("status")) {
            return "आपकी शिकायत का स्टेटस: प्रगति पर है";
        } else if (lower.contains("event")) {
            return "इवेंट पेज पर इवेंट उपलब्ध हैं";
        } else {
            return "मैं आपका प्रश्न समझ गया। हमारी टीम इस पर ध्यान देगी।";
        }
    }

    private String getSpanishToEnglishMock(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("problema") || lower.contains("queja")) {
            return "Complaint filed successfully";
        } else if (lower.contains("hola")) {
            return "Hello! How can I help you?";
        } else if (lower.contains("gracias")) {
            return "You're welcome!";
        } else if (lower.contains("ayuda")) {
            return "I'm here to help you";
        } else if (lower.contains("estado")) {
            return "Your complaint status is: In Progress";
        } else if (lower.contains("evento")) {
            return "Events are available on the Events page";
        } else {
            return "I understand your query. Our team will look into it.";
        }
    }

    private String getEnglishToSpanishMock(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("complaint")) {
            return "Queja registrada con éxito";
        } else if (lower.contains("hello") || lower.contains("hi")) {
            return "¡Hola! ¿Cómo puedo ayudarte?";
        } else if (lower.contains("thank")) {
            return "¡De nada!";
        } else if (lower.contains("help")) {
            return "Estoy aquí para ayudarte";
        } else if (lower.contains("status")) {
            return "El estado de tu queja es: En progreso";
        } else if (lower.contains("event")) {
            return "Los eventos están disponibles en la página de eventos";
        } else {
            return "Entiendo tu consulta. Nuestro equipo la revisará.";
        }
    }

    /**
     * Get supported languages for the UI
     */
    public Map<String, String> getSupportedLanguages() {
        return languageDetector.getSupportedLanguages();
    }
}