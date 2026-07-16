package com.vcsm.model.batch;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "batch_import_records",
       indexes = {
           @Index(name = "idx_job_id", columnList = "job_id"),
           @Index(name = "idx_status", columnList = "status")
       })
public class BatchImportRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private BatchImportJob job;

    @Column(nullable = false)
    private Integer rowNumber;  // Row number in the file

    @Column(length = 5000)
    private String rawData;  // Raw data from the file

    @Column(length = 5000)
    private String processedData;  // Processed/transformed data

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordStatus status;  // PENDING, PROCESSING, SUCCESS, FAILED

    @Column(length = 500)
    private String errorMessage;  // Error message if failed

    @Column
    private Long complaintId;  // ID of created complaint

    @Column
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;

    @Column
    private Long processingTimeMs;

    @Column(length = 100)
    private String validationErrors;  // JSON array of validation errors

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = RecordStatus.PENDING;
        }
    }

    public enum RecordStatus {
        PENDING,
        PROCESSING,
        SUCCESS,
        FAILED,
        SKIPPED
    }
}