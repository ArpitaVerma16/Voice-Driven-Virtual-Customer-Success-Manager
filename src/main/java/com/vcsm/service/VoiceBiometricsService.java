package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.model.VoicePrint;
import com.vcsm.model.VoiceVerificationResponse;
import com.vcsm.repository.UserRepository;
import com.vcsm.repository.VoicePrintRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.Optional;

@Service
public class VoiceBiometricsService {
    
    private static final double VERIFICATION_THRESHOLD = 0.75;
    private static final int SAMPLE_RATE = 16000;
    
    @Autowired
    private VoicePrintRepository voicePrintRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private VoiceFeatureService featureService;
    
    @Transactional
    public VoiceVerificationResponse enrollVoice(Long userId, String base64Audio, double durationSeconds) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isEmpty()) {
            return new VoiceVerificationResponse(false, 0, "User not found");
        }
        
        User user = userOpt.get();
        
        try {
            byte[] audioData = Base64.getDecoder().decode(base64Audio);
            double[] features = featureService.extractFeatures(audioData, SAMPLE_RATE);
            String featuresJson = featureService.featuresToJson(features);
            
            Optional<VoicePrint> existingPrint = voicePrintRepository.findByUser(user);
            
            if (existingPrint.isPresent()) {
                VoicePrint voicePrint = existingPrint.get();
                voicePrint.setVoiceFeatures(featuresJson);
                voicePrint.setSampleDuration(durationSeconds);
                voicePrintRepository.save(voicePrint);
            } else {
                VoicePrint voicePrint = new VoicePrint(user, featuresJson, durationSeconds);
                voicePrintRepository.save(voicePrint);
            }
            
            user.setVoiceEnrolled(true);
            userRepository.save(user);
            
            return new VoiceVerificationResponse(true, 1.0, 
                "Voice enrollment successful! You can now use voice commands.", userId, user.getName());
            
        } catch (Exception e) {
            return new VoiceVerificationResponse(false, 0, 
                "Enrollment failed: " + e.getMessage());
        }
    }
    
    @Transactional
    public VoiceVerificationResponse verifyVoice(Long userId, String base64Audio) {
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
        
        try {
            byte[] audioData = Base64.getDecoder().decode(base64Audio);
            double[] newFeatures = featureService.extractFeatures(audioData, SAMPLE_RATE);
            
            VoicePrint voicePrint = voicePrintOpt.get();
            double[] storedFeatures = featureService.jsonToFeatures(voicePrint.getVoiceFeatures());
            
            double similarity = featureService.calculateCosineSimilarity(newFeatures, storedFeatures);
            
            boolean verified = similarity >= VERIFICATION_THRESHOLD;
            String message = verified ? 
                "Voice verified successfully! Welcome " + user.getName() : 
                "Voice verification failed. Confidence: " + String.format("%.2f", similarity * 100) + "%";
            
            return new VoiceVerificationResponse(verified, similarity, message, userId, user.getName());
            
        } catch (Exception e) {
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