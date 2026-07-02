package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.model.VoiceProfile;
import com.vcsm.repository.VoiceProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class VoiceCloningService {

    @Autowired
    private VoiceProfileRepository voiceProfileRepository;

    @Autowired
    private com.vcsm.repository.SentimentAnalysisRepository sentimentAnalysisRepository;

    @Autowired
    private VoiceToneAdapterService voiceToneAdapterService;

    @Value("${voice.cloning.upload.dir:uploads/voices}")
    private String uploadDir;

    @Value("${elevenlabs.api.key:}")
    private String elevenLabsApiKey;

    private static final String ELEVENLABS_BASE_URL = "https://api.elevenlabs.io/v1";

    /**
     * Upload and clone voice sample
     */
    public VoiceProfile cloneVoice(User user, MultipartFile audioFile, String profileName) throws IOException {
        // Save audio file
        String filePath = saveAudioFile(audioFile, user.getId());

        // Create voice profile
        VoiceProfile profile = new VoiceProfile(user, profileName, filePath);
        
        // If this is the first profile, make it default
        List<VoiceProfile> existingProfiles = voiceProfileRepository.findByUserOrderByCreatedAtDesc(user);
        if (existingProfiles.isEmpty()) {
            profile.setDefault(true);
            profile.setActive(true);
        }

        // Call ElevenLabs API to clone voice (if API key is available)
        if (elevenLabsApiKey != null && !elevenLabsApiKey.isEmpty()) {
            String voiceId = callElevenLabsClone(audioFile, profileName);
            profile.setElevenLabsVoiceId(voiceId);
        }

        return voiceProfileRepository.save(profile);
    }

    /**
     * Save audio file to disk
     */
    private String saveAudioFile(MultipartFile audioFile, Long userId) throws IOException {
        Path uploadPath = Paths.get(uploadDir, userId.toString());
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalName = audioFile.getOriginalFilename();
        String safeName = originalName != null ? originalName.replaceAll("[^a-zA-Z0-9._-]", "_") : "audio";
        String fileName = UUID.randomUUID().toString() + "_" + safeName;
        Path filePath = uploadPath.resolve(fileName).normalize();
        if (!filePath.startsWith(uploadPath)) {
            throw new SecurityException("Path traversal detected");
        }
        Files.write(filePath, audioFile.getBytes());

        return filePath.toString();
    }

    /**
     * Call ElevenLabs API to clone voice
     */
    private String callElevenLabsClone(MultipartFile audioFile, String profileName) {
        // This is a placeholder - actual implementation would call ElevenLabs API
        // For demo purposes, return a mock voice ID
        return "voice_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Get all voice profiles for a user
     */
    public List<VoiceProfile> getUserProfiles(User user) {
        return voiceProfileRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get active voice profile for a user
     */
    public VoiceProfile getActiveProfile(User user) {
        return voiceProfileRepository.findByUserAndActiveTrue(user).orElse(null);
    }

    /**
     * Select a voice profile as active
     */
    public VoiceProfile selectProfile(User user, Long profileId) {
        // Deactivate all profiles
        List<VoiceProfile> profiles = voiceProfileRepository.findByUserOrderByCreatedAtDesc(user);
        for (VoiceProfile p : profiles) {
            p.setActive(false);
            voiceProfileRepository.save(p);
        }

        // Activate selected profile
        VoiceProfile selected = voiceProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Voice profile not found"));
        
        if (!selected.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        selected.setActive(true);
        return voiceProfileRepository.save(selected);
    }

    /**
     * Delete a voice profile
     */
    public void deleteProfile(User user, Long profileId) {
        VoiceProfile profile = voiceProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Voice profile not found"));
        
        if (!profile.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access");
        }

        // Delete audio file
        try {
            Files.deleteIfExists(Paths.get(profile.getVoiceSamplePath()));
        } catch (IOException e) {
            // Log error but continue
        }

        voiceProfileRepository.delete(profile);
    }

    /**
     * Synthesize speech using cloned voice
     */
    public byte[] synthesizeSpeech(User user, String text) {
        return synthesizeSpeech(user, text, null, null);
    }

    public byte[] synthesizeSpeech(User user, String text, String sentiment, Double confidence) {
        VoiceProfile activeProfile = getActiveProfile(user);
        
        // Retrieve sentiment if not provided
        if (sentiment == null || confidence == null) {
            List<com.vcsm.model.SentimentAnalysis> history = sentimentAnalysisRepository.findByUser(user);
            if (history != null && !history.isEmpty()) {
                com.vcsm.model.SentimentAnalysis latest = history.get(history.size() - 1);
                if (sentiment == null) sentiment = latest.getSentiment();
                if (confidence == null) confidence = latest.getConfidence();
            }
        }

        com.vcsm.model.AdaptiveVoiceSettings settings = voiceToneAdapterService.getAdaptiveSettings(sentiment, confidence);

        if (activeProfile == null) {
            // Return default TTS
            return synthesizeDefaultSpeech(text);
        }

        // If ElevenLabs voice ID is available, use it
        if (activeProfile.getElevenLabsVoiceId() != null && !activeProfile.getElevenLabsVoiceId().isEmpty()) {
            return synthesizeWithElevenLabs(activeProfile.getElevenLabsVoiceId(), text, settings);
        }

        return synthesizeDefaultSpeech(text);
    }

    /**
     * Synthesize speech with ElevenLabs
     */
    private byte[] synthesizeWithElevenLabs(String voiceId, String text) {
        return synthesizeWithElevenLabs(voiceId, text, new com.vcsm.model.AdaptiveVoiceSettings(0.75, 0.75, 0.0, true, 1.00));
    }

    private byte[] synthesizeWithElevenLabs(String voiceId, String text, com.vcsm.model.AdaptiveVoiceSettings settings) {
        System.out.printf("Synthesizing with ElevenLabs. VoiceId: %s, Text: '%s', Stability: %.2f, SimilarityBoost: %.2f, Style: %.2f, SpeakerBoost: %b, SpeedFactor: %.2f%n",
            voiceId, text, settings.getStability(), settings.getSimilarityBoost(), settings.getStyle(), settings.isUseSpeakerBoost(), settings.getSpeedFactor());
        
        String mockResponse = String.format("Audio: '%s' [voice=%s, stability=%.2f, speed=%.2f]", text, voiceId, settings.getStability(), settings.getSpeedFactor());
        return mockResponse.getBytes();
    }

    /**
     * Default TTS fallback
     */
    private byte[] synthesizeDefaultSpeech(String text) {
        // Placeholder for default TTS
        return text.getBytes();
    }

    /**
     * Check if user has any voice profiles
     */
    public boolean hasProfiles(User user) {
        return voiceProfileRepository.existsByUserAndActiveTrue(user);
    }
}