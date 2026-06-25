package com.vcsm.service;

import com.vcsm.model.AuditLog;
import com.vcsm.model.User;
import com.vcsm.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    public void logAction(User admin, String actionType, String description) {
        AuditLog log = new AuditLog(admin, actionType, description);
        auditLogRepository.save(log);
    }
    
    public void logAction(User admin, String actionType, String description, String targetType, Long targetId) {
        AuditLog log = new AuditLog(admin, actionType, description);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        auditLogRepository.save(log);
    }
    
    public void logAction(User admin, String actionType, String description, 
                          String targetType, Long targetId, String oldValue, String newValue) {
        AuditLog log = new AuditLog(admin, actionType, description);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        auditLogRepository.save(log);
    }
    
    public void logActionWithIp(User admin, String actionType, String description, HttpServletRequest request) {
        AuditLog log = new AuditLog(admin, actionType, description);
        if (request != null) {
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty()) {
                ip = request.getRemoteAddr();
            }
            log.setIpAddress(ip);
        }
        auditLogRepository.save(log);
    }
    
    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public List<AuditLog> getAuditLogsByAdmin(User admin) {
        return auditLogRepository.findByAdminOrderByCreatedAtDesc(admin);
    }
    
    public List<AuditLog> getAuditLogsByAction(String actionType) {
        return auditLogRepository.findByActionTypeOrderByCreatedAtDesc(actionType);
    }
    
    public List<AuditLog> getAuditLogsByTarget(String targetType, Long targetId) {
        return auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId);
    }
    
    public List<AuditLog> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }
}