package com.vcsm.model.dlq;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dead_letter_queue",
       indexes = {
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_created_at", columnList = "createdAt"),
           @Index(name = "idx_retry_count", columnList = "retryCount"),
           @Index(name = "idx_queue_name", columnList = "queueName")
       })
public class DeadLetterQueueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String queueName;  // e.g., "VOICE_PROCESSING", "EMAIL_SENDING"

    @Column(nullable = false, length = 50)
    private String messageId;  // Unique ID for the message

    @Column(nullable = false, length = 5000)
    private String payload;  // JSON payload of the failed task

    @Column(nullable = false)
    private String errorMessage;  // Error that caused failure

    @Column(nullable = false, length = 500)
    private String stackTrace;  // Stack trace of the error

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DLQStatus status;  // PENDING, RETRYING, RESOLVED, FAILED

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FailureReason failureReason;

    @Column(nullable = false)
    private Integer retryCount;

    @Column(nullable = false)
    private Integer maxRetries;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastRetryAt;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime resolvedAt;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime nextRetryAt;  // For exponential backoff

    @ElementCollection
    @CollectionTable(name = "dlq_retry_history", 
                     joinColumns = @JoinColumn(name = "dlq_entry_id"))
    private List<RetryAttempt> retryHistory;

    @Column
    private Long processingTimeMs;  // Time taken to process

    @Column(length = 50)
    private String source;  // Source of the message (e.g., "WEBSOCKET", "API")

    @Column(length = 100)
    private String userId;  // User who initiated the task

    @Column(length = 255)
    private String relatedEntityType;  // e.g., "COMPLAINT", "EVENT"

    @Column
    private Long relatedEntityId;

    @Column
    private String metadata;  // Additional metadata as JSON

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (retryCount == null) {
            retryCount = 0;
        }
        if (status == null) {
            status = DLQStatus.PENDING;
        }
        if (maxRetries == null) {
            maxRetries = 3;
        }
        if (retryHistory == null) {
            retryHistory = new ArrayList<>();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class RetryAttempt {
        private Integer attemptNumber;
        private LocalDateTime attemptTime;
        private String errorMessage;
        private Long durationMs;
        private Boolean success;
    }

    public enum DLQStatus {
        PENDING,      // Waiting for retry
        RETRYING,     // Currently retrying
        RESOLVED,     // Successfully processed
        FAILED,       // Max retries exceeded, manual intervention needed
        CANCELLED,    // Manually cancelled
        PROCESSING    // Currently being processed
    }

    public enum FailureReason {
        TIMEOUT,
        NETWORK_ERROR,
        UNEXPECTED_ERROR,
        VALIDATION_ERROR,
        RESOURCE_UNAVAILABLE,
        THIRD_PARTY_ERROR,
        VOICE_AI_ERROR,
        DATABASE_ERROR,
        RATE_LIMIT_EXCEEDED,
        UNKNOWN_ERROR
    }
}