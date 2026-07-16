package com.vcsm.controller.audit;

import com.vcsm.model.audit.AuditLogVerificationResult;
import com.vcsm.model.audit.ImmutableAuditLog;
import com.vcsm.service.ImmutableAuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final ImmutableAuditLogService auditLogService;

    /**
     * Get paginated audit logs
     */
    @GetMapping("/logs")
    public ResponseEntity<Page<ImmutableAuditLog>> getLogs(
            @PageableDefault(size = 50, sort = "sequenceNumber", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(auditLogService.getAuditLogs(pageable));
    }

    /**
     * Get audit trail for a specific entity
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<ImmutableAuditLog>> getEntityAuditTrail(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(auditLogService.getEntityAuditTrail(entityType, entityId));
    }

    /**
     * Get logs by action type
     */
    @GetMapping("/action/{action}")
    public ResponseEntity<List<ImmutableAuditLog>> getLogsByAction(@PathVariable String action) {
        return ResponseEntity.ok(auditLogService.getLogsByAction(action));
    }

    /**
     * Get logs by actor (user)
     */
    @GetMapping("/actor/{actor}")
    public ResponseEntity<List<ImmutableAuditLog>> getLogsByActor(@PathVariable String actor) {
        return ResponseEntity.ok(auditLogService.getLogsByActor(actor));
    }

    /**
     * Get logs between dates
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<ImmutableAuditLog>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(auditLogService.getLogsBetweenDates(start, end));
    }

    /**
     * Verify the entire audit chain
     */
    @GetMapping("/verify")
    public ResponseEntity<AuditLogVerificationResult> verifyChain() {
        AuditLogVerificationResult result = auditLogService.verifyChain();
        return result.getValid() ? ResponseEntity.ok(result) : ResponseEntity.status(409).body(result);
    }

    /**
     * Verify a range of logs
     */
    @GetMapping("/verify/range")
    public ResponseEntity<AuditLogVerificationResult> verifyRange(
            @RequestParam(required = false) Long startSequence,
            @RequestParam(required = false) Long endSequence) {
        return ResponseEntity.ok(auditLogService.verifyRange(startSequence, endSequence));
    }

    /**
     * Validate a single log entry
     */
    @GetMapping("/validate/{id}")
    public ResponseEntity<Map<String, Object>> validateLog(@PathVariable Long id) {
        boolean isValid = auditLogService.validateLog(id);
        return ResponseEntity.ok(Map.of(
            "logId", id,
            "valid", isValid,
            "message", isValid ? "Log entry is valid" : "Log entry has been tampered with!"
        ));
    }

    /**
     * Get chain summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getChainSummary() {
        return ResponseEntity.ok(auditLogService.getChainSummary());
    }

    /**
     * Export audit chain
     */
    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportChain(
            @RequestParam(defaultValue = "json") String format) {
        return ResponseEntity.ok(auditLogService.exportChain(format));
    }

    /**
     * Get chain statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> summary = auditLogService.getChainSummary();
        summary.put("verificationStatus", auditLogService.verifyChain().getValid());
        return ResponseEntity.ok(summary);
    }
}