package com.vcsm.service;

import com.vcsm.model.SupportedLanguage;
import org.springframework.stereotype.Service;

@Service
public class LanguageDetectionEnhancedService {

    private static final String HINDI_KEYWORDS = "क्या कहाँ कौन क्यूँ कैसे";
    private static final String SPANISH_KEYWORDS = "qué dónde quién por qué cómo";
    private static final String ENGLISH_KEYWORDS = "what where who why how";

    public SupportedLanguage detectLanguageFromAudio(String audioTranscript) {
        if (audioTranscript == null || audioTranscript.isEmpty()) {
            return SupportedLanguage.ENGLISH;
        }

        String normalized = audioTranscript.toLowerCase();
        int englishScore = countKeywords(normalized, ENGLISH_KEYWORDS);
        int hindiScore = countKeywords(normalized, HINDI_KEYWORDS);
        int spanishScore = countKeywords(normalized, SPANISH_KEYWORDS);

        if (hindiScore > englishScore && hindiScore > spanishScore) {
            return SupportedLanguage.HINDI;
        }
        if (spanishScore > englishScore && spanishScore > hindiScore) {
            return SupportedLanguage.SPANISH;
        }
        return SupportedLanguage.ENGLISH;
    }

    public boolean isSupportedLanguage(String languageCode) {
        try {
            SupportedLanguage.fromCode(languageCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private int countKeywords(String text, String keywords) {
        int count = 0;
        String[] words = keywords.split(" ");
        for (String word : words) {
            if (text.contains(word)) count++;
        }
        return count;
    }
}
