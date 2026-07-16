package com.vcsm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vcsm.model.User;
import com.vcsm.model.audit.AuditLogVerificationResult;
import com.vcsm.model.audit.ImmutableAuditLog;
import com.vcsm.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImmutableAuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    // Thread-safe lock for sequential hash generation
    private final ReentrantLock lock = new ReentrantLock();
    
    // Cache for quick verification
    private final Map<Long, String> hashCache = new ConcurrentHashMap<>();

    /**
     * Log an action with immutable hash chain
     */
    @Transactional
    public ImmutableAuditLog logAction(
            User actor,
            String action,
            String description,
            String entityType,
            Long entityId,
            String previousValue,
            String newValue,
            Boolean success,
            String errorMessage) {
        
        lock.lock();
        try {
            // Get the last log entry to link the chain
            Optional<ImmutableAuditLog> lastLog = auditLogRepository.findFirstByOrderBySequenceNumberDesc();
            
            // Determine sequence number
            int sequenceNumber = lastLog.map(log -> log.getSequenceNumber() + 1).orElse(1);
            
            // Get previous hash (empty string for first log)
            String previousHash = lastLog.map(ImmutableAuditLog::getCurrentHash).orElse("0000000000000000000000000000000000000000000000000000000000000000");
            
            // Get request details for context
            String ipAddress = getClientIP();
            String userAgent = getUserAgent();
            
            // Build the audit log
            ImmutableAuditLog auditLog = ImmutableAuditLog.builder()
                .actor(actor != null ? actor.getEmail() : "SYSTEM")
                .action(action)
                .description(description)
                .entityType(entityType)
                .entityId(entityId)
                .previousValue(previousValue)
                .newValue(newValue)
                .previousHash(previousHash)
                .sequenceNumber(sequenceNumber)
                .timestamp(LocalDateTime.now())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success != null ? success : true)
                .errorMessage(errorMessage)
                .build();
            
            // Generate current hash
            String currentHash = generateHash(auditLog);
            auditLog.setCurrentHash(currentHash);
            
            // Save the log
            ImmutableAuditLog saved = auditLogRepository.save(auditLog);
            
            // Update cache
            hashCache.put(saved.getId(), currentHash);
            
            log.info("✅ Audit log created: ID={}, Sequence={}, Hash={}", 
                saved.getId(), sequenceNumber, currentHash.substring(0, 16) + "...");
            
            return saved;
            
        } finally {
            lock.unlock();
        }
    }

    /**
     * Simplified log method for common use cases
     */
    @Transactional
    public ImmutableAuditLog logAction(
            User actor,
            String action,
            String description,
            String entityType,
            Long entityId) {
        return logAction(actor, action, description, entityType, entityId, null, null, true, null);
    }

    /**
     * Log with before/after values
     */
    @Transactional
    public ImmutableAuditLog logActionWithState(
            User actor,
            String action,
            String description,
            String entityType,
            Long entityId,
            Object previousState,
            Object newState) {
        
        try {
            String previousJson = previousState != null ? objectMapper.writeValueAsString(previousState) : null;
            String newJson = newState != null ? objectMapper.writeValueAsString(newState) : null;
            
            return logAction(actor, action, description, entityType, entityId, previousJson, newJson, true, null);
        } catch (Exception e) {
            log.error("Failed to serialize state for audit log", e);
            return logAction(actor, action, description, entityType, entityId, null, null, true, null);
        }
    }

    /**
     * Generate SHA-256 hash for a log entry
     */
    public String generateHash(ImmutableAuditLog log) {
        // Create hash string from all relevant fields
        String hashInput = String.format("%s|%s|%s|%s|%d|%s|%s|%s|%s|%d",
            log.getActor(),
            log.getAction(),
            log.getDescription(),
            log.getEntityType(),
            log.getEntityId(),
            log.getPreviousHash(),
            log.getSequenceNumber(),
            log.getTimestamp() != null ? log.getTimestamp().toString() : LocalDateTime.now().toString(),
            log.getPreviousValue() != null ? log.getPreviousValue() : "",
            log.getNewValue() != null ? log.getNewValue() : ""
        );
        
        // Generate SHA-256 hash
        return DigestUtils.sha256Hex(hashInput);
    }

    /**
     * Verify the entire audit log chain
     */
    public AuditLogVerificationResult verifyChain() {
        log.info("🔍 Starting full audit log chain verification...");
        
        List<ImmutableAuditLog> allLogs = auditLogRepository.findAllByOrderBySequenceNumberAsc(null)
            .getContent();
        
        return verifyChain(allLogs);
    }

    /**
     * Verify a specific chain
     */
    public AuditLogVerificationResult verifyChain(List<ImmutableAuditLog> logs) {
        if (logs.isEmpty()) {
            return AuditLogVerificationResult.builder()
                .valid(true)
                .message("No audit logs to verify")
                .verifiedAt(LocalDateTime.now())
                .totalEntries(0L)
                .corruptedEntries(0L)
                .tamperedEntries(0L)
                .build();
        }

        List<AuditLogVerificationResult.CorruptedEntry> corrupted = new ArrayList<>();
        String expectedHash = null;
        String lastValidHash = null;
        long corruptedCount = 0;
        long tamperedCount = 0;
        long totalCount = logs.size();

        for (int i = 0; i < logs.size(); i++) {
            ImmutableAuditLog log = logs.get(i);
            
            // Check if current hash matches calculated hash
            String calculatedHash = generateHash(log);
            boolean hashValid = calculatedHash.equals(log.getCurrentHash());
            
            // Check if chain link is valid (previous hash matches previous log's current hash)
            boolean chainValid = true;
            if (i > 0) {
                ImmutableAuditLog previousLog = logs.get(i - 1);
                chainValid = log.getPreviousHash().equals(previousLog.getCurrentHash());
            } else {
                // First log should have genesis hash
                chainValid = log.getPreviousHash().equals("0000000000000000000000000000000000000000000000000000000000000000");
            }
            
            if (!hashValid || !chainValid) {
                corruptedCount++;
                if (!hashValid) tamperedCount++;
                
                String issueType = !hashValid ? "TAMPERED" : "CHAIN_BREAK";
                
                corrupted.add(AuditLogVerificationResult.CorruptedEntry.builder()
                    .id(log.getId())
                    .currentHash(log.getCurrentHash())
                    .expectedHash(calculatedHash)
                    .previousHash(log.getPreviousHash())
                    .action(log.getAction())
                    .actor(log.getActor())
                    .timestamp(log.getTimestamp())
                    .issueType(issueType)
                    .build());
                
                if (lastValidHash == null) {
                    lastValidHash = i > 0 ? logs.get(i - 1).getCurrentHash() : null;
                }
                
                // Break chain at first corruption (for reporting)
                break;
            }
        }

        boolean isValid = corrupted.isEmpty();
        String message = isValid ? "✅ All audit logs verified successfully" : 
            "❌ Found " + corruptedCount + " corrupted log entries";

        if (!isValid && !corrupted.isEmpty()) {
            AuditLogVerificationResult.CorruptedEntry first = corrupted.get(0);
            message += " First corrupted at ID: " + first.getId() + 
                " (Issue: " + first.getIssueType() + ")";
        }

        return AuditLogVerificationResult.builder()
            .valid(isValid)
            .message(message)
            .verifiedAt(LocalDateTime.now())
            .totalEntries(totalCount)
            .corruptedEntries(corruptedCount)
            .tamperedEntries(tamperedCount)
            .corruptedLogs(corrupted)
            .firstCorruptedHash(corrupted.isEmpty() ? null : corrupted.get(0).getCurrentHash())
            .lastValidHash(lastValidHash)
            .build();
    }

    /**
     * Verify a specific range of logs
     */
    public AuditLogVerificationResult verifyRange(Long startSequence, Long endSequence) {
        List<ImmutableAuditLog> logs = auditLogRepository.findBySequenceNumberGreaterThanOrderBySequenceNumberAsc(
            startSequence != null ? startSequence.intValue() : 0);
        
        if (endSequence != null) {
            logs = logs.stream()
                .filter(log -> log.getSequenceNumber() <= endSequence)
                .collect(Collectors.toList());
        }
        
        return verifyChain(logs);
    }

    /**
     * Get audit trail for a specific entity
     */
    public List<ImmutableAuditLog> getEntityAuditTrail(String entityType, Long entityId) {
        return auditLogRepository.findByEntityTypeAndEntityIdOrderBySequenceNumberAsc(entityType, entityId);
    }

    /**
     * Get paginated audit logs
     */
    public Page<ImmutableAuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderBySequenceNumberDesc(pageable);
    }

    /**
     * Get audit logs by action type
     */
    public List<ImmutableAuditLog> getLogsByAction(String action) {
        return auditLogRepository.findByActionOrderByTimestampDesc(action);
    }

    /**
     * Get audit logs by actor
     */
    public List<ImmutableAuditLog> getLogsByActor(String actor) {
        return auditLogRepository.findByActorOrderByTimestampDesc(actor);
    }

    /**
     * Get audit logs between dates
     */
    public List<ImmutableAuditLog> getLogsBetweenDates(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByTimestampBetweenOrderByTimestampAsc(start, end);
    }

    /**
     * Export audit log chain in a verifiable format
     */
    public Map<String, Object> exportChain(String format) {
        Map<String, Object> export = new LinkedHashMap<>();
        List<ImmutableAuditLog> logs = auditLogRepository.findAllByOrderBySequenceNumberAsc(null).getContent();
        
        export.put("exportedAt", LocalDateTime.now());
        export.put("totalEntries", logs.size());
        export.put("chainStartHash", logs.isEmpty() ? null : logs.get(0).getPreviousHash());
        export.put("chainEndHash", logs.isEmpty() ? null : logs.get(logs.size() - 1).getCurrentHash());
        export.put("logs", logs);
        
        return export;
    }

    /**
     * Get chain summary
     */
    public Map<String, Object> getChainSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        
        long total = auditLogRepository.getTotalLogCount();
        Optional<ImmutableAuditLog> firstLog = auditLogRepository.findFirstByOrderBySequenceNumberAsc();
        Optional<ImmutableAuditLog> lastLog = auditLogRepository.findFirstByOrderBySequenceNumberDesc();
        
        summary.put("totalEntries", total);
        summary.put("firstEntry", firstLog.map(log -> Map.of(
            "id", log.getId(),
            "sequence", log.getSequenceNumber(),
            "timestamp", log.getTimestamp(),
            "action", log.getAction(),
            "hash", log.getCurrentHash()
        )).orElse(null));
        
        summary.put("lastEntry", lastLog.map(log -> Map.of(
            "id", log.getId(),
            "sequence", log.getSequenceNumber(),
            "timestamp", log.getTimestamp(),
            "action", log.getAction(),
            "hash", log.getCurrentHash()
        )).orElse(null));
        
        summary.put("lastHash", lastLog.map(ImmutableAuditLog::getCurrentHash).orElse(null));
        
        return summary;
    }

    /**
     * Validate a single log entry against the chain
     */
    public boolean validateLog(Long logId) {
        Optional<ImmutableAuditLog> logOpt = auditLogRepository.findById(logId);
        if (logOpt.isEmpty()) {
            return false;
        }
        
        ImmutableAuditLog log = logOpt.get();
        String calculatedHash = generateHash(log);
        return calculatedHash.equals(log.getCurrentHash());
    }

    // Helper methods
    private String getClientIP() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    // Additional methods for backward compatibility
    public ImmutableAuditLog logAction(
            User actor,
            String action,
            String description,
            String entityType,
            Long entityId,
            String previousValue,
            String newValue) {
        return logAction(actor, action, description, entityType, entityId, previousValue, newValue, true, null);
    }
}