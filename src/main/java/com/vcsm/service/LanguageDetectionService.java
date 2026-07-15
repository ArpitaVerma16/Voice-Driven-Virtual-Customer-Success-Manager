package com.vcsm.service;

import org.springframework.stereotype.Service;
import java.util.regex.Pattern;

@Service
public class LanguageDetectionService {
    
    // Hindi Unicode range: \u0900-\u097F
    private static final Pattern HINDI_PATTERN = Pattern.compile("[\\u0900-\\u097F]");
    
    // Common Hindi words
    private static final String[] HINDI_WORDS = {
        "शिकायत", "करो", "देखो", "बताओ", "क्या", "है", "मेरा", "मुझे", 
        "कृपया", "धन्यवाद", "नमस्ते", "इवेंट", "स्टेटस", "मदद"
    };
    
    public String detectLanguage(String text) {
        if (text == null || text.isEmpty()) {
            return org.springframework.http.ResponseEntity.ok("en");
        }
        
        // Check for Hindi characters
        if (HINDI_PATTERN.matcher(text).find()) {
            return org.springframework.http.ResponseEntity.ok("hi");
        }
        
        // Check for Hindi words even if Devanagari script not detected
        for (String hindiWord : HINDI_WORDS) {
            if (text.toLowerCase().contains(hindiWord.toLowerCase())) {
                return org.springframework.http.ResponseEntity.ok("hi");
            }
        }
        
        return org.springframework.http.ResponseEntity.ok("en");
    }
    
    public boolean isHindi(String text) {
        return detectLanguage(text).equals("hi");
    }
    
    public boolean isEnglish(String text) {
        return detectLanguage(text).equals("en");
    }
}