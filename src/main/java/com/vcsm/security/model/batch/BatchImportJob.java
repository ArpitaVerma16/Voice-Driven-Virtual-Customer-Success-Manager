package com.vcsm.model.batch;

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
@Table(name = "batch_import_jobs",
       indexes = {
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_created_at", columnList = "createdAt"),
           @Index(name = "idx_user_id", columnList = "userId")
       })
public class BatchImportJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String jobName;

    @Column(nullable = false, length = 50)
    private String jobId;  // Unique job identifier

    @Column(nullable = false)
    private String userId;  // User who initiated the import

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;  // PENDING, PROCESSING, COMPLETED, FAILED, PARTIAL

    @Column(nullable = false)
    private Integer totalRecords;

    @Column(nullable = false)
    private Integer processedRecords;

    @Column(nullable = false)
    private Integer successfulRecords;

    @Column(nullable = false)
    private Integer failedRecords;

    @Column
    private Integer progressPercentage;

    @Column
    private String filePath;  // Path to the uploaded file

    @Column
    private String fileName;  // Original file name

    @Column
    private String fileType;  // CSV, EXCEL, JSON

    @Column(length = 500)
    private String errorMessage;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startedAt;

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    @Column
    private Long processingTimeMs;  // Total processing time

    @Column
    private Integer batchSize;  // Size of each batch

    @Column
    private Integer currentBatchNumber;

    @Column
    private String metadata;  // Additional metadata as JSON

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BatchImportRecord> records = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = JobStatus.PENDING;
        }
        if (processedRecords == null) {
            processedRecords = 0;
        }
        if (successfulRecords == null) {
            successfulRecords = 0;
        }
        if (failedRecords == null) {
            failedRecords = 0;
        }
        if (progressPercentage == null) {
            progressPercentage = 0;
        }
        if (batchSize == null) {
            batchSize = 100;
        }
        if (currentBatchNumber == null) {
            currentBatchNumber = 0;
        }
    }

    public enum JobStatus {
        PENDING,        // Job created, waiting to start
        PROCESSING,     // Currently processing
        COMPLETED,      // All records processed successfully
        PARTIAL,        // Some records failed
        FAILED,         // Job failed
        CANCELLED,      // Cancelled by user
        PAUSED          // Temporarily paused
    }
}