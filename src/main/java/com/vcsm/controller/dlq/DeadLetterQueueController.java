package com.vcsm.controller.dlq;

import com.vcsm.model.dlq.DeadLetterQueueEntry;
import com.vcsm.model.dlq.DLQStatistics;
import com.vcsm.service.DeadLetterQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DeadLetterQueueController {

    private final DeadLetterQueueService dlqService;

    /**
     * Get all DLQ entries
     */
    @GetMapping("/entries")
    public ResponseEntity<Page<DeadLetterQueueEntry>> getEntries(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(dlqService.getAllEntries(pageable));
    }

    /**
     * Get entries by status
     */
    @GetMapping("/entries/status/{status}")
    public ResponseEntity<Page<DeadLetterQueueEntry>> getEntriesByStatus(
            @PathVariable DeadLetterQueueEntry.DLQStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dlqService.getEntriesByStatus(status, pageable));
    }

    /**
     * Get entries by queue
     */
    @GetMapping("/entries/queue/{queueName}")
    public ResponseEntity<Page<DeadLetterQueueEntry>> getEntriesByQueue(
            @PathVariable String queueName,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(dlqService.getEntriesByQueue(queueName, pageable));
    }

    /**
     * Get a single DLQ entry
     */
    @GetMapping("/entries/{id}")
    public ResponseEntity<DeadLetterQueueEntry> getEntry(@PathVariable Long id) {
        return dlqService.getAllEntries(Pageable.unpaged())
            .getContent().stream()
            .filter(e -> e.getId().equals(id))
            .findFirst()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get DLQ statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<DLQStatistics> getStats() {
        return ResponseEntity.ok(dlqService.getStatistics());
    }

    /**
     * Process retries
     */
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processRetries() {
        dlqService.processRetries();
        return ResponseEntity.ok(Map.of("message", "Retry processing started"));
    }

    /**
     * Manually replay a specific entry
     */
    @PostMapping("/replay/{id}")
    public ResponseEntity<Map<String, Object>> replayEntry(@PathVariable Long id) {
        boolean success = dlqService.replayManually(id);
        return ResponseEntity.ok(Map.of(
            "success", success,
            "message", success ? "Entry replayed successfully" : "Failed to replay entry"
        ));
    }

    /**
     * Bulk replay entries
     */
    @PostMapping("/replay/bulk")
    public ResponseEntity<Map<String, Object>> replayBulk(@RequestBody List<Long> ids) {
        Map<String, Object> result = dlqService.replayBulk(ids);
        return ResponseEntity.ok(result);
    }

    /**
     * Cancel a DLQ entry
     */
    @PostMapping("/cancel/{id}")
    public ResponseEntity<Map<String, Object>> cancelEntry(@PathVariable Long id) {
        dlqService.cancelEntry(id);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Entry cancelled successfully"
        ));
    }

    /**
     * Clean up old resolved entries
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanup(@RequestParam(defaultValue = "30") int retentionDays) {
        int deleted = dlqService.cleanupOldEntries(retentionDays);
        return ResponseEntity.ok(Map.of(
            "deleted", deleted,
            "message", "Cleaned up " + deleted + " old entries"
        ));
    }

    /**
     * Delete all failed entries
     */
    @DeleteMapping("/failed")
    public ResponseEntity<Map<String, Object>> deleteFailed() {
        int deleted = dlqService.deleteAllFailed();
        return ResponseEntity.ok(Map.of(
            "deleted", deleted,
            "message", "Deleted " + deleted + " failed entries"
        ));
    }

    /**
     * Get failure dashboard data
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        DLQStatistics stats = dlqService.getStatistics();
        
        Map<String, Object> dashboard = Map.of(
            "statistics", stats,
            "topFailures", stats.getByFailureReason().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .toList(),
            "status", Map.of(
                "healthy", stats.getFailed() == 0,
                "warning", stats.getFailed() > 0 && stats.getFailed() < 10,
                "critical", stats.getFailed() >= 10
            )
        );
        
        return ResponseEntity.ok(dashboard);
    }
}