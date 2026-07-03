package com.vcsm.repository;

import com.vcsm.model.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long>,
        JpaSpecificationExecutor<Complaint> {

    // =========================
    // Existing Queries
    // =========================

    List<Complaint> findByStatus(Complaint.ComplaintStatus status);

    List<Complaint> findByResidentName(String residentName);

    List<Complaint> findByResidentUsernameOrderByCreatedAtDesc(String residentUsername);

    Page<Complaint> findByResidentUsername(String residentUsername, Pageable pageable);

    List<Complaint> findByPriority(String priority);

    List<Complaint> findByPriorityOrderByCreatedAtAsc(String priority);

    Optional<Complaint> findByIdAndResidentUsername(Long id, String residentUsername);

    List<Complaint> findByCategory(Complaint.ComplaintCategory category);

    List<Complaint> findByApartmentNumber(String apartmentNumber);

    long countByStatus(Complaint.ComplaintStatus status);

    @Query("SELECT c.category, COUNT(c) FROM Complaint c GROUP BY c.category")
    List<Object[]> countByCategory();

    @Query("SELECT c FROM Complaint c ORDER BY c.createdAt DESC")
    List<Complaint> findAllOrderByCreatedAtDesc();

    @Query("SELECT c.priority, COUNT(c) FROM Complaint c GROUP BY c.priority")
    List<Object[]> countByPriority();

    @Query("SELECT c.id FROM Complaint c")
    List<Long> findAllIds();

    @Query("SELECT c.id FROM Complaint c WHERE c.status = :status")
    List<Long> findIdsByStatus(@Param("status") Complaint.ComplaintStatus status);

    Page<Complaint> findAll(Pageable pageable);

    // ======================================================
    // New Methods for Scheduled Complaint Escalation
    // ======================================================

    /**
     * Fetch all complaints that are still open.
     */
    List<Complaint> findByStatusOrderByCreatedAtAsc(
            Complaint.ComplaintStatus status
    );

    /**
     * Find complaints created before a given time.
     */
    List<Complaint> findByStatusAndCreatedAtBefore(
            Complaint.ComplaintStatus status,
            LocalDateTime createdAt
    );

    /**
     * Find complaints of a particular priority that are still open.
     */
    List<Complaint> findByStatusAndPriority(
            Complaint.ComplaintStatus status,
            String priority
    );

    /**
     * Count complaints by priority.
     */
    long countByPriority(String priority);

    /**
     * Count complaints by status and priority.
     */
    long countByStatusAndPriority(
            Complaint.ComplaintStatus status,
            String priority
    );

    /**
     * Fetch complaints eligible for escalation.
     */
    @Query("""
        SELECT c
        FROM Complaint c
        WHERE c.status IN ('OPEN','IN_PROGRESS')
        ORDER BY c.createdAt ASC
    """)
    List<Complaint> findEscalatableComplaints();

    /**
     * Fetch complaints older than a given time.
     */
    @Query("""
        SELECT c
        FROM Complaint c
        WHERE c.status = 'OPEN'
          AND c.createdAt <= :cutoff
        ORDER BY c.createdAt ASC
    """)
    List<Complaint> findOpenComplaintsOlderThan(
            @Param("cutoff") LocalDateTime cutoff
    );

    List<Complaint> findBySubCategory(Complaint.SubCategory subCategory);
}
