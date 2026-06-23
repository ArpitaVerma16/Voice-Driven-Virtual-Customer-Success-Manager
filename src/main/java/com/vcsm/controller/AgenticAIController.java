package com.vcsm.controller;

import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.ProactiveOutreachService;
import com.vcsm.service.UserBehaviorMonitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agentic")
@CrossOrigin(origins = "*")
public class AgenticAIController {

    @Autowired
    private UserBehaviorMonitor behaviorMonitor;

    @Autowired
    private ProactiveOutreachService outreachService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/analyze/{userId}")
    public ResponseEntity<UserBehaviorMonitor.BehaviorAnalysis> analyzeUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(behaviorMonitor.analyzeUserBehavior(user));
    }

    @PostMapping("/outreach/{userId}")
    public ResponseEntity<Map<String, Object>> sendOutreach(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "email") String channel) {
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        ProactiveOutreachService.OutreachResult result = 
            outreachService.sendProactiveOutreach(userId, channel);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        response.put("details", result.getDetails());
        response.put("userId", userId);
        response.put("channel", channel);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/at-risk")
    public ResponseEntity<List<UserBehaviorMonitor.BehaviorAnalysis>> getAtRiskUsers() {
        List<User> users = userRepository.findAll();
        List<UserBehaviorMonitor.BehaviorAnalysis> atRiskUsers = users.stream()
            .map(behaviorMonitor::analyzeUserBehavior)
            .filter(a -> a.getRiskScore() >= 40)
            .toList();
        return ResponseEntity.ok(atRiskUsers);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("status", "Agentic AI system running");
        stats.put("riskLevels", new String[]{"LOW", "MEDIUM", "HIGH", "CRITICAL"});
        stats.put("outreachChannels", new String[]{"Email", "SMS", "Voice"});
        return ResponseEntity.ok(stats);
    }
}