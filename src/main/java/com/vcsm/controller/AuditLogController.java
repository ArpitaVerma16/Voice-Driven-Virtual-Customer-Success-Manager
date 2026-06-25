package com.vcsm.controller;

import com.vcsm.model.AuditLog;
import com.vcsm.model.User;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@CrossOrigin(origins = "*")
public class AuditLogController {
    
    @Autowired
    private AuditLogService auditLogService;
    
    @Autowired
    private UserRepository userRepository;
    
    private User getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        String email = auth.getName();
        return userRepository.findByEmail(email).orElse(null);
    }
    
    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        User admin = getCurrentUser();
        if (admin == null || !isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditLogService.getAllAuditLogs());
    }
    
    @GetMapping("/logs/admin")
    public ResponseEntity<List<AuditLog>> getAdminLogs() {
        User admin = getCurrentUser();
        if (admin == null || !isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditLogService.getAuditLogsByAdmin(admin));
    }
    
    @GetMapping("/logs/action/{actionType}")
    public ResponseEntity<List<AuditLog>> getLogsByAction(@PathVariable String actionType) {
        User admin = getCurrentUser();
        if (admin == null || !isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditLogService.getAuditLogsByAction(actionType));
    }
    
    @GetMapping("/logs/target")
    public ResponseEntity<List<AuditLog>> getLogsByTarget(
            @RequestParam String targetType,
            @RequestParam Long targetId) {
        User admin = getCurrentUser();
        if (admin == null || !isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditLogService.getAuditLogsByTarget(targetType, targetId));
    }
    
    @GetMapping("/logs/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        User admin = getCurrentUser();
        if (admin == null || !isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        
        List<AuditLog> logs = auditLogService.getAllAuditLogs();
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", logs.size());
        
        // Count by action type
        Map<String, Integer> actionCounts = new HashMap<>();
        for (AuditLog log : logs) {
            actionCounts.put(log.getActionType(), actionCounts.getOrDefault(log.getActionType(), 0) + 1);
        }
        stats.put("byAction", actionCounts);
        
        return ResponseEntity.ok(stats);
    }
    
    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}