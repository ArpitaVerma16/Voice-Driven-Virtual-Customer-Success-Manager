package com.vcsm.repository;

import com.vcsm.model.Complaint;
import com.vcsm.model.EscalationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EscalationHistoryRepository
        extends JpaRepository<EscalationHistory, Long> {

    /**
     * Fetch escalation history for a complaint.
     */
    List<EscalationHistory> findByComplaintOrderByEscalatedAtDesc(
            Complaint complaint
    );

    /**
     * Fetch all escalations ordered by time.
     */
    List<EscalationHistory> findAllByOrderByEscalatedAtDesc();

    /**
     * Fetch escalations within a date range.
     */
    List<EscalationHistory> findByEscalatedAtBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * Fetch escalations by level.
     */
    List<EscalationHistory> findByEscalationLevel(
            Integer escalationLevel
    );

    /**
     * Count escalations for a complaint.
     */
    long countByComplaint(
            Complaint complaint
    );
}