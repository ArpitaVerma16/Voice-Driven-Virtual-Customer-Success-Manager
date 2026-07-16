package com.vcsm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vcsm.model.dlq.DeadLetterQueueEntry;
import com.vcsm.model.dlq.DLQStatistics;
import com.vcsm.repository.DeadLetterQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterQueueService {

    private final DeadLetterQueueRepository dlqRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, Function<Object, Boolean>> retryHandlers = new HashMap<>();
    private final AtomicInteger activeRetryCount = new AtomicInteger(0);

    /**
     * Register a retry handler for a specific queue
     */
    public void registerRetryHandler(String queueName, Function<Object, Boolean> handler) {
        retryHandlers.put(queueName, handler);
        log.info("✅ Registered retry handler for queue: {}", queueName);
    }

    /**
     * Add a message to DLQ
     */
    @Transactional
    public DeadLetterQueueEntry addToDeadLetterQueue(
            String queueName,
            String messageId,
            Object payload,
            Throwable error,
            String userId,
            String entityType,
            Long entityId,
            Map<String, Object> metadata) {

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            String stackTrace = getStackTraceAsString(error);
            
            DeadLetterQueueEntry entry = DeadLetterQueueEntry.builder()
                .queueName(queueName)
                .messageId(messageId)
                .payload(payloadJson)
                .errorMessage(error.getMessage())
                .stackTrace(stackTrace)
                .status(DeadLetterQueueEntry.DLQStatus.PENDING)
                .failureReason(determineFailureReason(error))
                .retryCount(0)
                .maxRetries(3)
                .createdAt(LocalDateTime.now())
                .nextRetryAt(calculateNextRetryTime(0))
                .userId(userId)
                .relatedEntityType(entityType)
                .relatedEntityId(entityId)
                .metadata(metadata != null ? objectMapper.writeValueAsString(metadata) : null)
                .source(determineSource())
                .retryHistory(new ArrayList<>())
                .build();

            DeadLetterQueueEntry saved = dlqRepository.save(entry);
            log.warn("📨 Message added to DLQ: ID={}, Queue={}, Error={}", 
                saved.getId(), queueName, error.getMessage());
            
            return saved;
        } catch (Exception e) {
            log.error("Failed to add message to DLQ", e);
            throw new RuntimeException("Failed to add to DLQ", e);
        }
    }

    /**
     * Simplified version for common use cases
     */
    @Transactional
    public DeadLetterQueueEntry addToDeadLetterQueue(
            String queueName,
            Object payload,
            Throwable error) {
        return addToDeadLetterQueue(
            queueName, 
            UUID.randomUUID().toString(), 
            payload, 
            error, 
            null, 
            null, 
            null, 
            null
        );
    }

    /**
     * Process pending retries
     */
    @Async
    public void processRetries() {
        log.info("🔄 Starting DLQ retry processing...");
        
        List<DeadLetterQueueEntry> pending = dlqRepository.findPendingRetries(LocalDateTime.now());
        
        if (pending.isEmpty()) {
            log.info("No pending retries found");
            return;
        }
        
        log.info("Found {} pending retries to process", pending.size());
        
        pending.forEach(this::processRetry);
    }

    /**
     * Process a single retry
     */
    @Transactional
    public void processRetry(DeadLetterQueueEntry entry) {
        if (activeRetryCount.get() >= 10) {
            log.warn("Max concurrent retries reached (10), skipping: {}", entry.getId());
            return;
        }

        activeRetryCount.incrementAndGet();
        
        try {
            log.info("🔄 Processing retry for DLQ entry: {}", entry.getId());
            
            // Update status
            entry.setStatus(DeadLetterQueueEntry.DLQStatus.RETRYING);
            entry.setLastRetryAt(LocalDateTime.now());
            entry.setRetryCount(entry.getRetryCount() + 1);
            dlqRepository.save(entry);
            
            // Get handler
            Function<Object, Boolean> handler = retryHandlers.get(entry.getQueueName());
            if (handler == null) {
                log.error("No handler registered for queue: {}", entry.getQueueName());
                markAsFailed(entry, "No handler registered for queue: " + entry.getQueueName());
                return;
            }
            
            // Parse payload
            Object payload = objectMapper.readValue(entry.getPayload(), Object.class);
            
            // Execute retry
            long startTime = System.currentTimeMillis();
            boolean success = handler.apply(payload);
            long duration = System.currentTimeMillis() - startTime;
            
            if (success) {
                // Success - mark as resolved
                markAsResolved(entry, duration);
                log.info("✅ Retry successful for DLQ entry: {}", entry.getId());
            } else {
                // Failed - record attempt
                recordRetryAttempt(entry, false, "Handler returned false", duration);
                
                if (entry.getRetryCount() >= entry.getMaxRetries()) {
                    markAsFailed(entry, "Max retries exceeded");
                    log.error("❌ DLQ entry failed permanently: {}", entry.getId());
                } else {
                    // Schedule next retry with exponential backoff
                    entry.setStatus(DeadLetterQueueEntry.DLQStatus.PENDING);
                    entry.setNextRetryAt(calculateNextRetryTime(entry.getRetryCount()));
                    dlqRepository.save(entry);
                    log.info("⏰ Scheduled next retry at: {}", entry.getNextRetryAt());
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing retry for DLQ entry: {}", entry.getId(), e);
            
            // Record failed attempt
            recordRetryAttempt(entry, false, e.getMessage(), null);
            
            if (entry.getRetryCount() >= entry.getMaxRetries()) {
                markAsFailed(entry, "Exception: " + e.getMessage());
            } else {
                try {
                    entry.setStatus(DeadLetterQueueEntry.DLQStatus.PENDING);
                    entry.setNextRetryAt(calculateNextRetryTime(entry.getRetryCount()));
                    dlqRepository.save(entry);
                } catch (Exception ex) {
                    log.error("Failed to update DLQ entry", ex);
                }
            }
            
        } finally {
            activeRetryCount.decrementAndGet();
        }
    }

    /**
     * Manual replay of a DLQ entry
     */
    @Transactional
    public boolean replayManually(Long entryId) {
        log.info("🔄 Manual replay requested for DLQ entry: {}", entryId);
        
        Optional<DeadLetterQueueEntry> entryOpt = dlqRepository.findById(entryId);
        if (entryOpt.isEmpty()) {
            log.error("DLQ entry not found: {}", entryId);
            return false;
        }
        
        DeadLetterQueueEntry entry = entryOpt.get();
        
        // Reset and process
        entry.setStatus(DeadLetterQueueEntry.DLQStatus.PENDING);
        entry.setRetryCount(0);
        entry.setNextRetryAt(LocalDateTime.now());
        entry.setResolvedAt(null);
        dlqRepository.save(entry);
        
        // Process immediately
        processRetry(entry);
        
        return true;
    }

    /**
     * Bulk replay of DLQ entries
     */
    @Transactional
    public Map<String, Object> replayBulk(List<Long> entryIds) {
        Map<String, Object> result = new HashMap<>();
        List<Long> success = new ArrayList<>();
        List<Long> failed = new ArrayList<>();
        
        for (Long id : entryIds) {
            try {
                boolean replayed = replayManually(id);
                if (replayed) {
                    success.add(id);
                } else {
                    failed.add(id);
                }
            } catch (Exception e) {
                failed.add(id);
                log.error("Failed to replay DLQ entry: {}", id, e);
            }
        }
        
        result.put("total", entryIds.size());
        result.put("success", success.size());
        result.put("failed", failed.size());
        result.put("successIds", success);
        result.put("failedIds", failed);
        
        return result;
    }

    /**
     * Get DLQ statistics
     */
    public DLQStatistics getStatistics() {
        long total = dlqRepository.count();
        long pending = dlqRepository.countPending();
        long failed = dlqRepository.countFailed();
        long resolved = dlqRepository.countResolved();
        
        Map<String, Long> byQueue = new HashMap<>();
        for (Object[] row : dlqRepository.countByQueue()) {
            byQueue.put((String) row[0], (Long) row[1]);
        }
        
        Map<String, Long> byReason = new HashMap<>();
        for (Object[] row : dlqRepository.countByFailureReason()) {
            byReason.put((String) row[0], (Long) row[1]);
        }
        
        Double avgRetries = dlqRepository.averageRetryCount();
        Long completed = dlqRepository.countCompleted();
        Long successful = dlqRepository.countSuccessful();
        
        double successRate = completed > 0 ? (double) successful / completed * 100 : 0;
        
        return DLQStatistics.builder()
            .totalEntries(total)
            .pending(pending)
            .retrying(0L) // Would need separate count
            .resolved(resolved)
            .failed(failed)
            .byQueue(byQueue)
            .byFailureReason(byReason)
            .averageRetryCount(avgRetries != null ? avgRetries : 0)
            .successRate(successRate)
            .build();
    }

    /**
     * Get DLQ entries by status
     */
    public Page<DeadLetterQueueEntry> getEntriesByStatus(DeadLetterQueueEntry.DLQStatus status, Pageable pageable) {
        return dlqRepository.findByStatus(status, pageable);
    }

    /**
     * Get DLQ entries by queue
     */
    public Page<DeadLetterQueueEntry> getEntriesByQueue(String queueName, Pageable pageable) {
        return dlqRepository.findByQueueName(queueName, pageable);
    }

    /**
     * Get all entries with pagination
     */
    public Page<DeadLetterQueueEntry> getAllEntries(Pageable pageable) {
        return dlqRepository.findAll(pageable);
    }

    /**
     * Cleanup old resolved entries
     */
    @Transactional
    public int cleanupOldEntries(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = dlqRepository.deleteResolvedOlderThan(cutoff);
        log.info("🧹 Cleaned up {} old resolved DLQ entries", deleted);
        return deleted;
    }

    /**
     * Delete all failed entries
     */
    @Transactional
    public int deleteAllFailed() {
        int deleted = dlqRepository.deleteAllFailedOrCancelled();
        log.info("🧹 Deleted {} failed/cancelled DLQ entries", deleted);
        return deleted;
    }

    /**
     * Cancel a DLQ entry
     */
    @Transactional
    public void cancelEntry(Long id) {
        DeadLetterQueueEntry entry = dlqRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("DLQ entry not found: " + id));
        entry.setStatus(DeadLetterQueueEntry.DLQStatus.CANCELLED);
        entry.setResolvedAt(LocalDateTime.now());
        dlqRepository.save(entry);
        log.info("🗑️ Cancelled DLQ entry: {}", id);
    }

    // Helper methods

    private void markAsResolved(DeadLetterQueueEntry entry, Long durationMs) {
        entry.setStatus(DeadLetterQueueEntry.DLQStatus.RESOLVED);
        entry.setResolvedAt(LocalDateTime.now());
        entry.setProcessingTimeMs(durationMs);
        recordRetryAttempt(entry, true, "Success", durationMs);
        dlqRepository.save(entry);
    }

    private void markAsFailed(DeadLetterQueueEntry entry, String reason) {
        entry.setStatus(DeadLetterQueueEntry.DLQStatus.FAILED);
        entry.setResolvedAt(LocalDateTime.now());
        entry.setErrorMessage(reason);
        dlqRepository.save(entry);
    }

    private void recordRetryAttempt(DeadLetterQueueEntry entry, boolean success, String message, Long durationMs) {
        DeadLetterQueueEntry.RetryAttempt attempt = DeadLetterQueueEntry.RetryAttempt.builder()
            .attemptNumber(entry.getRetryCount())
            .attemptTime(LocalDateTime.now())
            .errorMessage(message)
            .durationMs(durationMs)
            .success(success)
            .build();
        
        if (entry.getRetryHistory() == null) {
            entry.setRetryHistory(new ArrayList<>());
        }
        entry.getRetryHistory().add(attempt);
        dlqRepository.save(entry);
    }

    private LocalDateTime calculateNextRetryTime(int retryCount) {
        // Exponential backoff: 2^retryCount * 60 seconds
        long delaySeconds = (long) Math.pow(2, retryCount) * 60;
        // Cap at 24 hours
        delaySeconds = Math.min(delaySeconds, 86400);
        return LocalDateTime.now().plusSeconds(delaySeconds);
    }

    private DeadLetterQueueEntry.FailureReason determineFailureReason(Throwable error) {
        String message = error.getMessage().toLowerCase();
        if (message.contains("timeout")) return DeadLetterQueueEntry.FailureReason.TIMEOUT;
        if (message.contains("network")) return DeadLetterQueueEntry.FailureReason.NETWORK_ERROR;
        if (message.contains("validation")) return DeadLetterQueueEntry.FailureReason.VALIDATION_ERROR;
        if (message.contains("rate limit")) return DeadLetterQueueEntry.FailureReason.RATE_LIMIT_EXCEEDED;
        if (message.contains("database") || message.contains("sql")) return DeadLetterQueueEntry.FailureReason.DATABASE_ERROR;
        if (message.contains("voice") || message.contains("omnidim")) return DeadLetterQueueEntry.FailureReason.VOICE_AI_ERROR;
        return DeadLetterQueueEntry.FailureReason.UNKNOWN_ERROR;
    }

    private String getStackTraceAsString(Throwable error) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : error.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 500) break;  // Limit length
        }
        return sb.toString();
    }

    private String determineSource() {
        // In a real implementation, could detect source from context
        return "SYSTEM";
    }
}
