package com.vcsm.service;

import com.vcsm.model.SupportedLanguage;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MultilingualResponseService {

    private static final Map<SupportedLanguage, Map<String, String>> RESPONSES = Map.of(
        SupportedLanguage.ENGLISH, Map.of(
            "welcome", "Welcome to VCSM. How can I help you?",
            "goodbye", "Thank you for contacting us. Goodbye!",
            "error", "I apologize, there was an error processing your request."
        ),
        SupportedLanguage.HINDI, Map.of(
            "welcome", "VCSM में आपका स्वागत है। मैं आपकी कैसे मदद कर सकता हूं?",
            "goodbye", "हमसे संपर्क करने के लिए धन्यवाद। अलविदा!",
            "error", "क्षमा करें, आपके अनुरोध को संसाधित करने में त्रुटि हुई।"
        ),
        SupportedLanguage.SPANISH, Map.of(
            "welcome", "Bienvenido a VCSM. ¿Cómo puedo ayudarte?",
            "goodbye", "Gracias por contactarnos. ¡Adiós!",
            "error", "Disculpe, hubo un error al procesar su solicitud."
        )
    );

    public String getResponse(SupportedLanguage language, String responseKey) {
        Map<String, String> languageResponses = RESPONSES.getOrDefault(language, RESPONSES.get(SupportedLanguage.ENGLISH));
        return languageResponses.getOrDefault(responseKey, "Service unavailable in this language");
    }

    public String translateMessage(String message, SupportedLanguage targetLanguage) {
        if (targetLanguage == SupportedLanguage.ENGLISH) {
            return message;
        }
        Map<String, String> translations = RESPONSES.get(targetLanguage);
        for (Map.Entry<String, String> entry : translations.entrySet()) {
            if (entry.getValue().equals(message)) {
                return entry.getValue();
            }
        }
        return message;
    }
}
