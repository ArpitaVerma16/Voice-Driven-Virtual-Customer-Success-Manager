package com.vcsm.repository;

import com.vcsm.model.AuditLog;
import com.vcsm.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();
    
    List<AuditLog> findAllByOrderByCreatedAtDesc();
    
    List<AuditLog> findByAdminOrderByCreatedAtDesc(User admin);
    
    List<AuditLog> findByActionTypeOrderByCreatedAtDesc(String actionType);
    
    List<AuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(String targetType, Long targetId);
    
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT a FROM AuditLog a WHERE a.adminEmail = :email ORDER BY a.createdAt DESC")
    List<AuditLog> findByAdminEmail(@Param("email") String email);
    
    @Query("SELECT a FROM AuditLog a WHERE a.actionType IN :actions ORDER BY a.createdAt DESC")
    List<AuditLog> findByActionTypes(@Param("actions") List<String> actions);
}
