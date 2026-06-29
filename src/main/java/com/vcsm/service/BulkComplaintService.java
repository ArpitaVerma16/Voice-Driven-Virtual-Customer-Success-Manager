package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

@Service
public class BulkComplaintService {

    private static final Logger log = Logger.getLogger(BulkComplaintService.class.getName());

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private User getCurrentUser() {
        String username = currentUsername();
        if (username == null) return null;
        return userRepository.findByEmail(username).orElse(null);
    }

    /**
     * Bulk resolve complaints
     */
    @Transactional
    public Map<String, Object> bulkResolve(List<Long> complaintIds, String resolutionNotes) {
        if (!isAdmin()) {
            throw new RuntimeException("Only admins can perform bulk operations");
        }

        Map<String, Object> result = new HashMap<>();
        List<Long> processed = new ArrayList<>();
        List<Long> failed = new ArrayList<>();
        int successCount = 0;

        User admin = getCurrentUser();

        for (Long id : complaintIds) {
            try {
                Complaint complaint = complaintRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));

                // Only resolve if not already resolved
                if (complaint.getStatus() != Complaint.ComplaintStatus.RESOLVED) {
                    complaint.setStatus(Complaint.ComplaintStatus.RESOLVED);
                    complaint.setResolvedBy(admin != null ? admin.getName() : "Admin");
                    if (resolutionNotes != null && !resolutionNotes.isBlank()) {
                        complaint.setResolutionNotes(resolutionNotes);
                    }
                    complaint.setUpdatedAt(LocalDateTime.now());
                    complaintRepository.save(complaint);
                    successCount++;
                    processed.add(id);

                    // Log activity
                    if (admin != null) {
                        userActivityService.logActivity(
                            admin,
                            "BULK_RESOLVE",
                            "Resolved complaint #" + id + " in bulk operation",
                            id
                        );
                    }
                } else {
                    processed.add(id);
                }
            } catch (Exception e) {
                failed.add(id);
                log.warning("Failed to resolve complaint " + id + ": " + e.getMessage());
            }
        }

        result.put("success", successCount);
        result.put("total", complaintIds.size());
        result.put("processed", processed);
        result.put("failed", failed);
        result.put("message", "Successfully resolved " + successCount + " out of " + complaintIds.size() + " complaints");

        return result;
    }

    /**
     * Bulk update complaint status
     */
    @Transactional
    public Map<String, Object> bulkUpdateStatus(List<Long> complaintIds, String newStatus) {
        if (!isAdmin()) {
            throw new RuntimeException("Only admins can perform bulk operations");
        }

        Complaint.ComplaintStatus targetStatus = Complaint.ComplaintStatus.valueOf(newStatus.toUpperCase());
        Map<String, Object> result = new HashMap<>();
        List<Long> processed = new ArrayList<>();
        List<Long> failed = new ArrayList<>();
        int successCount = 0;

        User admin = getCurrentUser();

        for (Long id : complaintIds) {
            try {
                Complaint complaint = complaintRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));

                Complaint.ComplaintStatus oldStatus = complaint.getStatus();
                complaint.setStatus(targetStatus);
                complaint.setUpdatedAt(LocalDateTime.now());
                complaintRepository.save(complaint);
                successCount++;
                processed.add(id);

                // Log activity
                if (admin != null) {
                    userActivityService.logActivity(
                        admin,
                        "BULK_STATUS_UPDATE",
                        "Updated complaint #" + id + " from " + oldStatus + " to " + targetStatus + " (bulk)",
                        id
                    );
                }
            } catch (Exception e) {
                failed.add(id);
                log.warning("Failed to update complaint " + id + ": " + e.getMessage());
            }
        }

        result.put("success", successCount);
        result.put("total", complaintIds.size());
        result.put("processed", processed);
        result.put("failed", failed);
        result.put("newStatus", targetStatus.toString());
        result.put("message", "Successfully updated " + successCount + " out of " + complaintIds.size() + " complaints");

        return result;
    }

    /**
     * Get all complaint IDs for bulk selection
     */
    public List<Long> getAllComplaintIds() {
        return complaintRepository.findAllIds();
    }

    /**
     * Get complaint IDs by status
     */
    public List<Long> getComplaintIdsByStatus(String status) {
        Complaint.ComplaintStatus targetStatus = Complaint.ComplaintStatus.valueOf(status.toUpperCase());
        return complaintRepository.findIdsByStatus(targetStatus);
    }
}