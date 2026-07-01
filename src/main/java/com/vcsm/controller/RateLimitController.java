package com.vcsm.controller;

import com.vcsm.service.RateLimiterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ratelimit")
public class RateLimitController {

    @Autowired
    private RateLimiterService rateLimiterService;

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@RequestParam String userId) {
        RateLimiterService.RateLimitStatus status = rateLimiterService.getStatus(userId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("remaining", status.getRemaining());
        response.put("limit", status.getLimit());
        response.put("canConsume", status.isCanConsume());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetLimit(@RequestParam String userId) {
        rateLimiterService.resetLimit(userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Rate limit reset for user: " + userId
        ));
    }
}