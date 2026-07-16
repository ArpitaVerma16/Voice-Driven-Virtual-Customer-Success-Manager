package com.vcsm.model.audit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "immutable_audit_logs")
@EntityListeners(AuditingEntityListener.class)
public class ImmutableAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private String actor;  // User who performed the action

    @Column(nullable = false, updatable = false)
    private String action;  // Action performed (e.g., "UPDATE_STATUS")

    @Column(nullable = false, updatable = false, length = 500)
    private String description;  // Detailed description

    @Column(nullable = false, updatable = false)
    private String entityType;  // Type of entity (e.g., "COMPLAINT", "EVENT")

    @Column(nullable = false, updatable = false)
    private Long entityId;  // ID of the affected entity

    @Column(updatable = false)
    private String previousValue;  // Previous state (JSON)

    @Column(updatable = false)
    private String newValue;  // New state (JSON)

    @Column(nullable = false, updatable = false, length = 64)
    private String previousHash;  // Hash of the previous log entry (hex)

    @Column(nullable = false, updatable = false, unique = true, length = 64)
    private String currentHash;  // Hash of this log entry (hex)

    @Column(nullable = false, updatable = false)
    private Integer sequenceNumber;  // Sequential order in the chain

    @CreatedDate
    @Column(nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    @Column(updatable = false)
    private String ipAddress;  // IP address of the requester

    @Column(updatable = false)
    private String userAgent;  // User agent of the requester

    @Column(updatable = false)
    private Boolean success;  // Whether the action succeeded

    @Column(updatable = false)
    private String errorMessage;  // Error message if failed

    @Transient
    private Boolean isVerified;  // For verification results (not stored)

    @PrePersist
    protected void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
        if (sequenceNumber == null) {
            sequenceNumber = 0;
        }
    }

    // Builder with hash generation
    public static class ImmutableAuditLogBuilder {
        public ImmutableAuditLog build() {
            ImmutableAuditLog log = new ImmutableAuditLog();
            log.actor = this.actor;
            log.action = this.action;
            log.description = this.description;
            log.entityType = this.entityType;
            log.entityId = this.entityId;
            log.previousValue = this.previousValue;
            log.newValue = this.newValue;
            log.previousHash = this.previousHash;
            log.currentHash = this.currentHash;
            log.sequenceNumber = this.sequenceNumber;
            log.timestamp = this.timestamp;
            log.ipAddress = this.ipAddress;
            log.userAgent = this.userAgent;
            log.success = this.success;
            log.errorMessage = this.errorMessage;
            
            // Auto-generate hash if not provided
            if (log.currentHash == null) {
                log.currentHash = generateHash(log);
            }
            return log;
        }
        
        private String generateHash(ImmutableAuditLog log) {
            // Implementation in the service class
            return null;
        }
    }
}