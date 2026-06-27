package com.vcsm.repository;

import com.vcsm.model.AuditLog;
import com.vcsm.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // ===========================
    // Existing Methods
    // ===========================

    List<AuditLog> findAllByOrderByCreatedAtDesc();

    List<AuditLog> findByAdminOrderByCreatedAtDesc(User admin);

    List<AuditLog> findByActionTypeOrderByCreatedAtDesc(String actionType);

    List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
            String targetType,
            Long targetId);

    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime start,
            LocalDateTime end);

    @Query("SELECT a FROM AuditLog a WHERE a.adminEmail = :email ORDER BY a.createdAt DESC")
    List<AuditLog> findByAdminEmail(@Param("email") String email);

    @Query("SELECT a FROM AuditLog a WHERE a.actionType IN :actions ORDER BY a.createdAt DESC")
    List<AuditLog> findByActionTypes(@Param("actions") List<String> actions);

    // ===========================
    // Pagination Support
    // ===========================

    Page<AuditLog> findAll(Pageable pageable);

    // ===========================
    // Filtering with Pagination
    // ===========================

    Page<AuditLog> findByActionType(
            String actionType,
            Pageable pageable);

    Page<AuditLog> findByStatus(
            String status,
            Pageable pageable);

    Page<AuditLog> findByUsername(
            String username,
            Pageable pageable);

    Page<AuditLog> findByCreatedAtBetween(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable);

    Page<AuditLog> findByTargetType(
            String targetType,
            Pageable pageable);

    // ===========================
    // Search Queries
    // ===========================

    @Query("""
            SELECT a
            FROM AuditLog a
            WHERE LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%'))
            """)
    Page<AuditLog> searchByUsername(
            @Param("username") String username,
            Pageable pageable);

    @Query("""
            SELECT a
            FROM AuditLog a
            WHERE LOWER(a.endpoint) LIKE LOWER(CONCAT('%', :endpoint, '%'))
            """)
    Page<AuditLog> searchByEndpoint(
            @Param("endpoint") String endpoint,
            Pageable pageable);

    @Query("""
            SELECT a
            FROM AuditLog a
            WHERE LOWER(a.actionType) LIKE LOWER(CONCAT('%', :actionType, '%'))
            """)
    Page<AuditLog> searchByActionType(
            @Param("actionType") String actionType,
            Pageable pageable);

    @Query("""
            SELECT a
            FROM AuditLog a
            WHERE LOWER(a.status) LIKE LOWER(CONCAT('%', :status, '%'))
            """)
    Page<AuditLog> searchByStatus(
            @Param("status") String status,
            Pageable pageable);
}