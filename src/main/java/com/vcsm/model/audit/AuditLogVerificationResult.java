package com.vcsm.model.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogVerificationResult {
    
    private Boolean valid;
    private String message;
    private LocalDateTime verifiedAt;
    private Long totalEntries;
    private Long corruptedEntries;
    private Long tamperedEntries;
    private String firstCorruptedHash;
    private String lastValidHash;
    private List<CorruptedEntry> corruptedLogs;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CorruptedEntry {
        private Long id;
        private String currentHash;
        private String expectedHash;
        private String previousHash;
        private String action;
        private String actor;
        private LocalDateTime timestamp;
        private String issueType; // "INVALID_HASH", "TAMPERED", "MISSING"
    }
}