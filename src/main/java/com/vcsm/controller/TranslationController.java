package com.vcsm.controller;

import com.vcsm.service.LanguageDetector;
import com.vcsm.service.VoiceTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/translation")
public class TranslationController {

    @Autowired
    private VoiceTranslationService translationService;

    @Autowired
    private LanguageDetector languageDetector;

    @PostMapping("/translate")
    public ResponseEntity<Map<String, Object>> translate(
            @Valid @RequestBody Map<String, String> request) {
        
        String text = request.get("text");
        String targetLang = request.get("targetLang");
        String sourceLang = request.getOrDefault("sourceLang", "auto");

        if (text == null || text.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Text is required")
            );
        }

        if (targetLang == null || targetLang.isEmpty()) {
            targetLang = "en";
        }

        String translatedText;
        if ("auto".equals(sourceLang)) {
            translatedText = translationService.autoTranslate(text, targetLang);
            sourceLang = languageDetector.detectLanguage(text);
        } else {
            translatedText = translationService.translateText(text, sourceLang, targetLang);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("original", text);
        response.put("translated", translatedText);
        response.put("sourceLanguage", sourceLang);
        response.put("targetLanguage", targetLang);
        response.put("sourceLanguageName", languageDetector.getLanguageName(sourceLang));
        response.put("targetLanguageName", languageDetector.getLanguageName(targetLang));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/detect")
    public ResponseEntity<Map<String, String>> detectLanguage(
            @Valid @RequestBody Map<String, String> request) {
        
        String text = request.get("text");
        if (text == null || text.isEmpty()) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Text is required")
            );
        }

        String code = languageDetector.detectLanguage(text);
        String name = languageDetector.getLanguageName(code);

        Map<String, String> response = new HashMap<>();
        response.put("languageCode", code);
        response.put("languageName", name);
        response.put("display", languageDetector.getLanguageDisplay(text));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/languages")
    public ResponseEntity<Map<String, String>> getLanguages() {
        return ResponseEntity.ok(translationService.getSupportedLanguages());
    }
}