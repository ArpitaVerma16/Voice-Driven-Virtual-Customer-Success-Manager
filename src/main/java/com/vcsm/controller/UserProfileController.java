package com.vcsm.controller;

import com.vcsm.model.User;
import com.vcsm.model.UserActivity;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.UserActivityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserActivityService userActivityService;
    
    @GetMapping("/{id}/profile")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("phone", user.getPhone());
        profile.put("profileImage", user.getProfileImage());
        profile.put("preferredLanguage", user.getPreferredLanguage());
        profile.put("isVoiceEnrolled", user.isVoiceEnrolled());
        profile.put("lastActive", user.getLastActive());
        profile.put("createdAt", user.getCreatedAt());
        
        return ResponseEntity.ok(profile);
    }
    
    @PutMapping("/{id}/profile")
    @PreAuthorize("#id == authentication.principal.id or hasRole('ADMIN')")
    public ResponseEntity<?> updateProfile(@PathVariable Long id, @Valid @RequestBody Map<String, String> updates) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        
        if (updates.containsKey("name")) {
            user.setName(updates.get("name"));
        }
        if (updates.containsKey("phone")) {
            user.setPhone(updates.get("phone"));
        }
        if (updates.containsKey("email")) {
            user.setEmail(updates.get("email"));
        }
        if (updates.containsKey("profileImage")) {
            user.setProfileImage(updates.get("profileImage"));
        }
        
        userRepository.save(user);
        
        // Log activity
        userActivityService.logActivity(user, "PROFILE_UPDATE", "Updated profile information", null);
        
        return ResponseEntity.ok(Map.of("success", true, "message", "Profile updated successfully"));
    }
    
    @GetMapping("/{id}/activity")
    public ResponseEntity<?> getActivity(@PathVariable Long id, Pageable pageable) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Page<UserActivity> activities = userActivityService.getUserActivities(userOpt.get(), pageable);
        return ResponseEntity.ok(activities);
    }
    
    @GetMapping("/{id}/activity/{type}")
    public ResponseEntity<?> getActivityByType(@PathVariable Long id, @PathVariable String type) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<UserActivity> activities = userActivityService.getUserActivitiesByType(userOpt.get(), type);
        return ResponseEntity.ok(activities);
    }
    
    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getUserStats(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalActivities", userActivityService.getActivityCount(user));
        stats.put("isVoiceEnrolled", user.isVoiceEnrolled());
        stats.put("lastActive", user.getLastActive());
        stats.put("memberSince", user.getCreatedAt());
        
        return ResponseEntity.ok(stats);
    }
}