package com.vcsm.controller.batch;

import com.vcsm.model.batch.BatchImportJob;
import com.vcsm.model.batch.BatchImportRecord;
import com.vcsm.service.BatchImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchImportController {

    private final BatchImportService batchImportService;

    /**
     * Start a batch import
     */
    @PostMapping("/import")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BatchImportJob> startImport(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "jobName", required = false) String jobName,
            @RequestParam(value = "batchSize", required = false) Integer batchSize) {
        
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        BatchImportJob job = batchImportService.startBatchImport(file, userId, jobName, batchSize);
        return ResponseEntity.accepted().body(job);
    }

    /**
     * Get job status
     */
    @GetMapping("/jobs/{jobId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BatchImportJob> getJobStatus(@PathVariable String jobId) {
        BatchImportJob job = batchImportService.getJobStatus(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * Get job progress
     */
    @GetMapping("/jobs/{jobId}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getJobProgress(@PathVariable String jobId) {
        int progress = batchImportService.getJobProgress(jobId);
        BatchImportJob job = batchImportService.getJobStatus(jobId);
        return ResponseEntity.ok(Map.of(
            "jobId", jobId,
            "progress", progress,
            "status", job.getStatus(),
            "processed", job.getProcessedRecords(),
            "total", job.getTotalRecords(),
            "success", job.getSuccessfulRecords(),
            "failed", job.getFailedRecords()
        ));
    }

    /**
     * Get jobs for current user
     */
    @GetMapping("/jobs")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<BatchImportJob>> getMyJobs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        Page<BatchImportJob> jobs = batchImportService.getJobsForUser(userId, pageable);
        return ResponseEntity.ok(jobs);
    }

    /**
     * Get all jobs (admin)
     */
    @GetMapping("/admin/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BatchImportJob>> getAllJobs(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(batchImportService.getAllJobs(pageable));
    }

    /**
     * Get records for a job
     */
    @GetMapping("/jobs/{jobId}/records")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<BatchImportRecord>> getJobRecords(
            @PathVariable Long jobId,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<BatchImportRecord> records = batchImportService.getJobRecords(jobId, pageable);
        return ResponseEntity.ok(records);
    }

    /**
     * Cancel a job
     */
    @PostMapping("/jobs/{jobId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> cancelJob(@PathVariable String jobId) {
        boolean cancelled = batchImportService.cancelJob(jobId);
        return ResponseEntity.ok(Map.of(
            "jobId", jobId,
            "cancelled", cancelled,
            "message", cancelled ? "Job cancelled successfully" : "Job could not be cancelled"
        ));
    }

    /**
     * Retry failed records
     */
    @PostMapping("/jobs/{jobId}/retry")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BatchImportJob> retryFailedRecords(@PathVariable Long jobId) {
        BatchImportJob job = batchImportService.retryFailedRecords(jobId);
        return ResponseEntity.ok(job);
    }

    /**
     * Get summary statistics
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(batchImportService.getSummaryStats());
    }
}
