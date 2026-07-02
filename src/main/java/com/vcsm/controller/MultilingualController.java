package com.vcsm.controller;

import com.vcsm.model.SupportedLanguage;
import com.vcsm.service.LanguageDetectionEnhancedService;
import com.vcsm.service.MultilingualResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/languages")
@CrossOrigin(origins = "*")
public class MultilingualController {

    @Autowired
    private LanguageDetectionEnhancedService languageDetectionService;

    @Autowired
    private MultilingualResponseService multilingualResponseService;

    @GetMapping("/supported")
    public ResponseEntity<Map<String, Object>> getSupportedLanguages() {
        List<Map<String, String>> languages = new ArrayList<>();
        for (SupportedLanguage lang : SupportedLanguage.values()) {
            languages.add(Map.of(
                "code", lang.getCode(),
                "name", lang.getDisplayName()
            ));
        }
        return ResponseEntity.ok(Map.of(
            "supported_languages", languages,
            "count", languages.size(),
            "success", true
        ));
    }

    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detectLanguage(@Valid @RequestBody Map<String, String> request) {
        String audioTranscript = request.get("transcript");

        if (audioTranscript == null || audioTranscript.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Transcript is required",
                "success", false
            ));
        }

        SupportedLanguage detectedLanguage = languageDetectionService.detectLanguageFromAudio(audioTranscript);

        return ResponseEntity.ok(Map.of(
            "detected_language", detectedLanguage.getCode(),
            "language_name", detectedLanguage.getDisplayName(),
            "is_supported", true,
            "success", true
        ));
    }

    @PostMapping("/response")
    public ResponseEntity<Map<String, Object>> getResponse(@Valid @RequestBody Map<String, String> request) {
        String languageCode = request.get("language");
        String responseKey = request.get("responseKey");

        if (languageCode == null || responseKey == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "language and responseKey required",
                "success", false
            ));
        }

        SupportedLanguage language = SupportedLanguage.fromCode(languageCode);
        String response = multilingualResponseService.getResponse(language, responseKey);

        return ResponseEntity.ok(Map.of(
            "language", language.getCode(),
            "response", response,
            "success", true
        ));
    }

    @GetMapping("/check/{languageCode}")
    public ResponseEntity<Map<String, Object>> isLanguageSupported(@PathVariable String languageCode) {
        boolean supported = languageDetectionService.isSupportedLanguage(languageCode);
        return ResponseEntity.ok(Map.of(
            "language_code", languageCode,
            "is_supported", supported,
            "success", true
        ));
    }
}

