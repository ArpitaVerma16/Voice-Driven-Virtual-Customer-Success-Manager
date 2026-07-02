package com.vcsm.controller;

import com.vcsm.service.PersonalizationEngine;
import com.vcsm.service.RecommendationService;
import com.vcsm.service.UserProfileBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/personalize")
@lombok.RequiredArgsConstructor
public class PersonalizationController {

    private final UserProfileBuilder userProfileBuilder;

    private final RecommendationService recommendationService;

    private final PersonalizationEngine personalizationEngine;

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfileBuilder.UserProfile> getProfile(@PathVariable Long userId) {
        UserProfileBuilder.UserProfile profile = userProfileBuilder.buildProfile(userId);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/recommendations/{userId}")
    public ResponseEntity<RecommendationService.Recommendations> getRecommendations(@PathVariable Long userId) {
        return ResponseEntity.ok(recommendationService.getRecommendations(userId));
    }

    @GetMapping("/experience/{userId}")
    public ResponseEntity<PersonalizationEngine.PersonalizedExperience> getPersonalizedExperience(@PathVariable Long userId) {
        PersonalizationEngine.PersonalizedExperience experience = 
            personalizationEngine.getPersonalizedExperience(userId);
        if (experience == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(experience);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
            "status", "Hyper-Personalization Engine running",
            "features", new String[]{"Profile Building", "Recommendations", "Dynamic UI", "Adaptive Learning"}
        ));
    }
}