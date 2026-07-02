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

/**
 * REST controller for managing audit log operations and audit statistics.
 *
 * <p>All endpoints require the authenticated user to have administrator
 * privileges.</p>
 */
@RestController
@RequestMapping("/api/audit")
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

    /**
     * Retrieves all audit logs.
     *
     * @return a response containing all audit logs if the user is an administrator,
     *         or a 403 Forbidden response otherwise
     */
    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAllLogs() {
        User admin = getCurrentUser();
        if (admin == null || !isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditLogService.getAllAuditLogs());
    }

    /**
     * Retrieves audit logs created by the currently authenticated administrator.
     *
     * @return a response containing the administrator's audit logs if authorized,
     *         or a 403 Forbidden response otherwise
     */
    @GetMapping("/logs/admin")
    public ResponseEntity<List<AuditLog>> getAdminLogs() {
        User admin = getCurrentUser();
        if (admin == null || !isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditLogService.getAuditLogsByAdmin(admin));
    }

    /**
     * Retrieves audit logs for a specific action type.
     *
     * @param actionType the action type used to filter audit logs
     * @return a response containing matching audit logs if authorized,
     *         or a 403 Forbidden response otherwise
     */
    @GetMapping("/logs/action/{actionType}")
    public ResponseEntity<List<AuditLog>> getLogsByAction(@PathVariable String actionType) {
        User admin = getCurrentUser();
        if (admin == null || !isAdmin()) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditLogService.getAuditLogsByAction(actionType));
    }

    /**
     * Retrieves audit logs associated with a specific target entity.
     *
     * @param targetType the type of the target entity
     * @param targetId the identifier of the target entity
     * @return a response containing matching audit logs if authorized,
     *         or a 403 Forbidden response otherwise
     */
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

    /**
     * Retrieves summary statistics for audit logs.
     *
     * @return a response containing audit log statistics if authorized,
     *         or a 403 Forbidden response otherwise
     */
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