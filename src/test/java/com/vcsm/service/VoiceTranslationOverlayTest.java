package com.vcsm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@lombok.RequiredArgsConstructor
public class VoiceTranslationOverlayTest {

    private final MockMvc mockMvc;

    private final VoiceTranslationService translationService;

    private final LanguageDetector languageDetector;

    private final ObjectMapper objectMapper;

    @Test
    public void testSpanishDetection() {
        String spanishText = "Hola, necesito ayuda con mi queja.";
        String detected = languageDetector.detectLanguage(spanishText);
        assertEquals("es", detected);
        assertEquals("Spanish", languageDetector.getLanguageName(detected));
    }

    @Test
    public void testSpanishTranslationFallback() throws Exception {
        // Test translation from Spanish to English
        mockMvc.perform(post("/api/translation/translate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "text", "Hola, necesito ayuda",
                        "sourceLang", "es",
                        "targetLang", "en"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translated").value("Hello! How can I help you?"));

        // Test translation from English to Spanish
        mockMvc.perform(post("/api/translation/translate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "text", "Hello, I have a complaint",
                        "sourceLang", "en",
                        "targetLang", "es"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translated").value("Queja registrada con éxito"));
    }

    @Test
    public void testHindiTranslationFallback() throws Exception {
        // Test translation from Hindi to English
        mockMvc.perform(post("/api/translation/translate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "text", "नमस्ते, मेरी पानी की मोटर खराब है",
                        "sourceLang", "hi",
                        "targetLang", "en"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.translated").value("Hello! How can I help you?"));
    }

    @Test
    public void testAutoDetectionAndTranslation() throws Exception {
        // Text contains Spanish keyword 'hola'
        mockMvc.perform(post("/api/translation/translate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "text", "Hola",
                        "sourceLang", "auto",
                        "targetLang", "en"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceLanguage").value("es"))
                .andExpect(jsonPath("$.translated").value("Hello! How can I help you?"));
    }
}
