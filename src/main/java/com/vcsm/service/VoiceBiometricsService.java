package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.model.VoicePrint;
import com.vcsm.model.VoiceVerificationResponse;
import com.vcsm.repository.UserRepository;
import com.vcsm.repository.VoicePrintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Base64;
import java.util.Optional;

@Service
public class VoiceBiometricsService {

    private static final Logger log = LoggerFactory.getLogger(VoiceBiometricsService.class);

    private static final double VERIFICATION_THRESHOLD = 0.75;
    private static final int SAMPLE_RATE = 16000;
    
    @Autowired
    private VoicePrintRepository voicePrintRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VoiceFeatureService featureService;

    @Autowired
    private LanguageDetectionService languageDetectionService;
    
    @Transactional
    public VoiceVerificationResponse enrollVoice(Long userId, String base64Audio, double durationSeconds) {
        return enrollVoice(userId, base64Audio, durationSeconds, "en", "My voice is my secure password");
    }

    @Transactional
    public VoiceVerificationResponse enrollVoice(Long userId, String base64Audio, double durationSeconds, String language, String text) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            return new VoiceVerificationResponse(false, 0, "User not found");
        }
        
        User user = userOpt.get();
        String targetLanguage = (language != null && !language.isBlank()) ? language.toLowerCase() : "en";
        
        if (text == null || text.isBlank()) {
            return new VoiceVerificationResponse(false, 0, 
                "Enrollment failed: Verification text is required");
        }
        
        String detectedLanguage = languageDetectionService.detectLanguage(text);
        if (!detectedLanguage.equalsIgnoreCase(targetLanguage)) {
            return new VoiceVerificationResponse(false, 0, 
                "Enrollment failed: Voice pattern does not match the chosen language context (" + targetLanguage.toUpperCase() + ")");
        }
        
        try {
            byte[] audioData = Base64.getDecoder().decode(base64Audio);
            double[] features = featureService.extractFeatures(audioData, SAMPLE_RATE);
            String featuresJson = featureService.featuresToJson(features);
            
            Optional<VoicePrint> existingPrint = voicePrintRepository.findByUser(user);
            
            if (existingPrint.isPresent()) {
                VoicePrint voicePrint = existingPrint.get();
                voicePrint.setVoiceFeatures(featuresJson);
                voicePrint.setSampleDuration(durationSeconds);
                voicePrint.setLanguage(targetLanguage);
                voicePrintRepository.save(voicePrint);
            } else {
                VoicePrint voicePrint = new VoicePrint(user, featuresJson, durationSeconds, targetLanguage);
                voicePrintRepository.save(voicePrint);
            }
            
            user.setVoiceEnrolled(true);
            userRepository.save(user);
            
            return new VoiceVerificationResponse(true, 1.0, 
                "Voice enrollment successful! You can now use voice commands.", userId, user.getName());
            
        } catch (Exception e) {
            log.error("Voice enrollment failed for user {}: {}", userId, e.getMessage(), e);
            return new VoiceVerificationResponse(false, 0, 
                "Enrollment failed: " + e.getMessage());
        }
    }
    
    @Transactional
    public VoiceVerificationResponse verifyVoice(Long userId, String base64Audio) {
        return verifyVoice(userId, base64Audio, "My voice is my secure password");
    }

    @Transactional
    public VoiceVerificationResponse verifyVoice(Long userId, String base64Audio, String text) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            return new VoiceVerificationResponse(false, 0, "User not found");
        }
        
        User user = userOpt.get();
        
        if (!user.isVoiceEnrolled()) {
            return new VoiceVerificationResponse(false, 0, 
                "User not enrolled. Please enroll your voice first.");
        }
        
        Optional<VoicePrint> voicePrintOpt = voicePrintRepository.findByUser(user);
        
        if (voicePrintOpt.isEmpty()) {
            return new VoiceVerificationResponse(false, 0, 
                "Voice print not found. Please enroll your voice first.");
        }
        
        VoicePrint voicePrint = voicePrintOpt.get();
        String enrolledLanguage = voicePrint.getLanguage();
        if (enrolledLanguage == null || enrolledLanguage.isBlank()) {
            enrolledLanguage = "en";
        }
        
        if (text == null || text.isBlank()) {
            return new VoiceVerificationResponse(false, 0, 
                "Verification failed: Verification text is required");
        }
        
        String detectedLanguage = languageDetectionService.detectLanguage(text);
        if (!detectedLanguage.equalsIgnoreCase(enrolledLanguage)) {
            return new VoiceVerificationResponse(false, 0, 
                "Verification failed: Spoken phrase language (" + detectedLanguage.toUpperCase() + 
                ") does not match enrolled language context (" + enrolledLanguage.toUpperCase() + ")");
        }
        
        try {
            byte[] audioData = Base64.getDecoder().decode(base64Audio);
            double[] newFeatures = featureService.extractFeatures(audioData, SAMPLE_RATE);
            
            double[] storedFeatures = featureService.jsonToFeatures(voicePrint.getVoiceFeatures());
            
            double similarity = featureService.calculateCosineSimilarity(newFeatures, storedFeatures);
            
            boolean verified = similarity >= VERIFICATION_THRESHOLD;
            String message = verified ? 
                "Voice verified successfully! Welcome " + user.getName() : 
                "Voice verification failed. Confidence: " + String.format("%.2f", similarity * 100) + "%";
            
            return new VoiceVerificationResponse(verified, similarity, message, userId, user.getName());
            
        } catch (Exception e) {
            log.error("Voice verification failed for user {}: {}", userId, e.getMessage(), e);
            return new VoiceVerificationResponse(false, 0, 
                "Verification failed: " + e.getMessage());
        }
    }
    
    public boolean isVoiceEnrolled(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        return userOpt.isPresent() && userOpt.get().isVoiceEnrolled();
    }
    
    @Transactional
    public VoiceVerificationResponse deleteVoicePrint(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            return new VoiceVerificationResponse(false, 0, "User not found");
        }
        
        User user = userOpt.get();
        Optional<VoicePrint> voicePrintOpt = voicePrintRepository.findByUser(user);
        
        if (voicePrintOpt.isPresent()) {
            voicePrintRepository.delete(voicePrintOpt.get());
            user.setVoiceEnrolled(false);
            userRepository.save(user);
            return new VoiceVerificationResponse(true, 0, "Voice print deleted successfully");
        }
        
        return new VoiceVerificationResponse(false, 0, "No voice print found for user");
    }
}