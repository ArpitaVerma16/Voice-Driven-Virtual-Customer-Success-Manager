package com.vcsm.repository;

import com.vcsm.model.dlq.DeadLetterQueueEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeadLetterQueueRepository extends 
    JpaRepository<DeadLetterQueueEntry, Long>, 
    JpaSpecificationExecutor<DeadLetterQueueEntry> {

    // Find by status
    List<DeadLetterQueueEntry> findByStatus(DeadLetterQueueEntry.DLQStatus status);
    
    Page<DeadLetterQueueEntry> findByStatus(DeadLetterQueueEntry.DLQStatus status, Pageable pageable);
    
    // Find pending messages ready for retry
    @Query("SELECT d FROM DeadLetterQueueEntry d WHERE d.status = 'PENDING' AND d.nextRetryAt <= :now")
    List<DeadLetterQueueEntry> findPendingRetries(@Param("now") LocalDateTime now);
    
    // Find by failure reason
    List<DeadLetterQueueEntry> findByFailureReason(DeadLetterQueueEntry.FailureReason reason);
    
    // Find by queue name
    Page<DeadLetterQueueEntry> findByQueueName(String queueName, Pageable pageable);
    
    // Find by user
    List<DeadLetterQueueEntry> findByUserId(String userId);
    
    // Find by entity
    List<DeadLetterQueueEntry> findByRelatedEntityTypeAndRelatedEntityId(String entityType, Long entityId);
    
    // Statistics queries
    @Query("SELECT COUNT(d) FROM DeadLetterQueueEntry d WHERE d.status = 'PENDING'")
    long countPending();
    
    @Query("SELECT COUNT(d) FROM DeadLetterQueueEntry d WHERE d.status = 'FAILED'")
    long countFailed();
    
    @Query("SELECT COUNT(d) FROM DeadLetterQueueEntry d WHERE d.status = 'RESOLVED'")
    long countResolved();
    
    @Query("SELECT d.queueName, COUNT(d) FROM DeadLetterQueueEntry d GROUP BY d.queueName")
    List<Object[]> countByQueue();
    
    @Query("SELECT d.failureReason, COUNT(d) FROM DeadLetterQueueEntry d GROUP BY d.failureReason")
    List<Object[]> countByFailureReason();
    
    @Query("SELECT AVG(d.retryCount) FROM DeadLetterQueueEntry d WHERE d.status = 'RESOLVED' OR d.status = 'FAILED'")
    Double averageRetryCount();
    
    @Query("SELECT COUNT(d) FROM DeadLetterQueueEntry d WHERE d.status = 'RESOLVED' OR d.status = 'FAILED'")
    Long countCompleted();
    
    @Query("SELECT COUNT(d) FROM DeadLetterQueueEntry d WHERE d.status = 'RESOLVED'")
    Long countSuccessful();
    
    // Delete old entries
    @Modifying
    @Transactional
    @Query("DELETE FROM DeadLetterQueueEntry d WHERE d.resolvedAt < :cutoffDate AND d.status = 'RESOLVED'")
    int deleteResolvedOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM DeadLetterQueueEntry d WHERE d.status = 'FAILED' OR d.status = 'CANCELLED'")
    int deleteAllFailedOrCancelled();
    
    // Update status
    @Modifying
    @Transactional
    @Query("UPDATE DeadLetterQueueEntry d SET d.status = :status, d.resolvedAt = :now WHERE d.id = :id")
    int updateStatus(@Param("id") Long id, 
                     @Param("status") DeadLetterQueueEntry.DLQStatus status,
                     @Param("now") LocalDateTime now);
    
    // Find oldest pending
    Optional<DeadLetterQueueEntry> findFirstByStatusOrderByCreatedAtAsc(DeadLetterQueueEntry.DLQStatus status);
}