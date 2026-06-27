package com.vcsm.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class LanguageDetector {

    // Hindi Unicode range: \u0900-\u097F
    private static final Pattern HINDI_PATTERN = Pattern.compile("[\\u0900-\\u097F]");
    
    // Language codes mapping
    private static final Map<String, String> LANGUAGE_NAMES = new HashMap<>();
    private static final Map<String, String> LANGUAGE_CODES = new HashMap<>();
    
    static {
        LANGUAGE_NAMES.put("hi", "Hindi");
        LANGUAGE_NAMES.put("en", "English");
        LANGUAGE_NAMES.put("es", "Spanish");
        LANGUAGE_NAMES.put("ta", "Tamil");
        LANGUAGE_NAMES.put("te", "Telugu");
        LANGUAGE_NAMES.put("ml", "Malayalam");
        LANGUAGE_NAMES.put("kn", "Kannada");
        LANGUAGE_NAMES.put("bn", "Bengali");
        LANGUAGE_NAMES.put("mr", "Marathi");
        LANGUAGE_NAMES.put("gu", "Gujarati");
        LANGUAGE_NAMES.put("pa", "Punjabi");
        
        LANGUAGE_CODES.put("Hindi", "hi");
        LANGUAGE_CODES.put("English", "en");
        LANGUAGE_CODES.put("Spanish", "es");
        LANGUAGE_CODES.put("Tamil", "ta");
        LANGUAGE_CODES.put("Telugu", "te");
        LANGUAGE_CODES.put("Malayalam", "ml");
        LANGUAGE_CODES.put("Kannada", "kn");
        LANGUAGE_CODES.put("Bengali", "bn");
        LANGUAGE_CODES.put("Marathi", "mr");
        LANGUAGE_CODES.put("Gujarati", "gu");
        LANGUAGE_CODES.put("Punjabi", "pa");
    }

    public String detectLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return "en";
        }

        // Check for Spanish vocabulary/accents
        String lower = text.toLowerCase();
        if (lower.contains("hola") || lower.contains("gracias") || 
            lower.contains("queja") || lower.contains("ayuda") ||
            lower.contains("estado") || lower.contains("evento") ||
            lower.contains("problema")) {
            return "es";
        }
        
        // Check for Hindi characters
        if (HINDI_PATTERN.matcher(text).find()) {
            return "hi";
        }
        
        // Check for Tamil characters (Tamil Unicode range: \u0B80-\u0BFF)
        if (Pattern.compile("[\\u0B80-\\u0BFF]").matcher(text).find()) {
            return "ta";
        }
        
        // Check for Telugu characters (Telugu Unicode range: \u0C00-\u0C7F)
        if (Pattern.compile("[\\u0C00-\\u0C7F]").matcher(text).find()) {
            return "te";
        }
        
        // Check for Malayalam characters (Malayalam Unicode range: \u0D00-\u0D7F)
        if (Pattern.compile("[\\u0D00-\\u0D7F]").matcher(text).find()) {
            return "ml";
        }
        
        // Check for Kannada characters (Kannada Unicode range: \u0C80-\u0CFF)
        if (Pattern.compile("[\\u0C80-\\u0CFF]").matcher(text).find()) {
            return "kn";
        }
        
        // Check for Bengali characters (Bengali Unicode range: \u0980-\u09FF)
        if (Pattern.compile("[\\u0980-\\u09FF]").matcher(text).find()) {
            return "bn";
        }
        
        // Default to English
        return "en";
    }

    public String getLanguageName(String code) {
        return LANGUAGE_NAMES.getOrDefault(code, "Unknown");
    }

    public String getLanguageCode(String name) {
        return LANGUAGE_CODES.getOrDefault(name, "en");
    }

    public Map<String, String> getSupportedLanguages() {
        return new HashMap<>(LANGUAGE_NAMES);
    }

    public boolean isHindi(String text) {
        return detectLanguage(text).equals("hi");
    }

    public boolean isEnglish(String text) {
        return detectLanguage(text).equals("en");
    }

    public String getLanguageDisplay(String text) {
        String code = detectLanguage(text);
        return LANGUAGE_NAMES.getOrDefault(code, "Unknown") + " (" + code + ")";
    }
}