package com.vcsm.controller;

import com.vcsm.service.LanguageDetectionService;
import com.vcsm.service.HindiCommandMapper;
import com.vcsm.model.VoiceCommand;
import com.vcsm.service.OmnidimService;
import com.vcsm.service.SentimentAnalysisService;
import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voice")
@CrossOrigin(origins = "*")
public class VoiceController {

    private static final int MAX_TRANSCRIPT_LENGTH = 2000;

    @Autowired
    private OmnidimService omnidimService;

    @Autowired
    private SentimentAnalysisService sentimentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LanguageDetectionService languageDetectionService;

    @Autowired
    private HindiCommandMapper hindiCommandMapper;

    @PostMapping("/command")
    public ResponseEntity<Map<String, Object>> command(
            @RequestBody Map<String, String> body) {

        String transcript = body.get("transcript");

        // Validate empty transcript
        if (transcript == null || transcript.isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", "Transcript required",
                            "success", false
                    )
            );
        }

        // Reject oversized transcripts before expensive processing
        if (transcript.length() > MAX_TRANSCRIPT_LENGTH) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", "Transcript too long",
                            "message", "Transcript must not exceed "
                                    + MAX_TRANSCRIPT_LENGTH
                                    + " characters",
                            "success", false
                    )
            );
        }

        // Authentication check FIRST
        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || auth.getName() == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("status", 401);
            error.put("error", "Unauthorized");
            error.put("message", "Authentication required");
            error.put("success", false);

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(error);
        }

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found: " + auth.getName()
                        )
                );

        // Detect language
        String language =
                languageDetectionService.detectLanguage(transcript);

        Map<String, Object> response = new HashMap<>();

        response.put("originalText", transcript);
        response.put("detectedLanguage", language);

        // Process based on language
        if ("hi".equals(language)) {

            String action =
                    hindiCommandMapper.mapCommand(transcript);

            if (action != null) {
                response.put("action", action);
                response.put(
                        "response",
                        hindiCommandMapper.getResponse(action, null)
                );
                response.put("success", true);
            } else {
                response.put("action", "unknown");
                response.put(
                        "response",
                        hindiCommandMapper.getDefaultResponse()
                );
                response.put("success", false);
            }

        } else {

            Map<String, Object> englishResponse =
                    omnidimService.processVoiceCommand(transcript);

            response.putAll(englishResponse);
            response.put("success", true);
        }

        // Analyze sentiment dynamically from authenticated user
        Long userId = user.getId();

        sentimentService.analyzeAndProcess(
                userId,
                transcript
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<VoiceCommand>> history() {
        return ResponseEntity.ok(
                omnidimService.getRecentCommands()
        );
    }
}