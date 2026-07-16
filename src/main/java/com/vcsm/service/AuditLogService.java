package com.vcsm.service;

import com.vcsm.model.User;
import com.vcsm.model.audit.ImmutableAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final ImmutableAuditLogService immutableAuditLogService;

    /**
     * Legacy method - delegates to immutable audit log
     */
    public void logAction(User actor, String action, String description, 
                         String entityType, Long entityId) {
        immutableAuditLogService.logAction(
            actor, action, description, entityType, entityId
        );
    }

    /**
     * Legacy method with state
     */
    public void logAction(User actor, String action, String description, 
                         String entityType, Long entityId,
                         String previousValue, String newValue) {
        immutableAuditLogService.logAction(
            actor, action, description, entityType, entityId, 
            previousValue, newValue
        );
    }

    /**
     * New method with full context
     */
    public ImmutableAuditLog logImmutableAction(
            User actor,
            String action,
            String description,
            String entityType,
            Long entityId,
            Object previousState,
            Object newState) {
        return immutableAuditLogService.logActionWithState(
            actor, action, description, entityType, entityId, 
            previousState, newState
        );
    }
}