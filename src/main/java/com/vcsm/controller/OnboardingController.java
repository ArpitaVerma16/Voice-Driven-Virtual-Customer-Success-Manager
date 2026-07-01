package com.vcsm.controller;

import com.vcsm.service.OnboardingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    @Autowired
    private OnboardingService onboardingService;

    @GetMapping("/steps")
    public ResponseEntity<List<Map<String, Object>>> getSteps() {
        return ResponseEntity.ok(onboardingService.getTutorialSteps());
    }

    @GetMapping("/step/{id}")
    public ResponseEntity<Map<String, Object>> getStep(@PathVariable int id) {
        Map<String, Object> step = onboardingService.getStep(id);
        if (step != null) {
            return ResponseEntity.ok(step);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/complete")
    public ResponseEntity<Map<String, String>> completeOnboarding() {
        // Mark onboarding as complete for user
        Map<String, String> response = new HashMap<>();
        response.put("success", "true");
        response.put("message", "Onboarding completed successfully");
        return ResponseEntity.ok(response);
    }
}
