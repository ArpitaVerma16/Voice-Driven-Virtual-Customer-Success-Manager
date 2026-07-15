package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import com.vcsm.service.elasticsearch.ElasticsearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;
import java.util.logging.Logger;

@Service
public class ComplaintService {

    private static final Logger log = Logger.getLogger(ComplaintService.class.getName());

    @Autowired
    private ComplaintRepository complaintRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private PriorityClassifierService priorityClassifierService;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlockchainService blockchainService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    private boolean isAdmin() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
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

    private User getComplaintUser(Long complaintId) {
        Optional<Complaint> complaintOpt = complaintRepository.findById(complaintId);
        if (complaintOpt.isEmpty()) return null;
        Complaint complaint = complaintOpt.get();
        String username = complaint.getResidentUsername();
        if (username == null || username.isEmpty()) return null;
        return userRepository.findByEmail(username).orElse(null);
    }

    @Transactional
    public Complaint fileComplaint(Complaint complaint) {
        String username = currentUsername();
        if (username == null) throw new RuntimeException("Unauthorized");

        complaint.setResidentUsername(username);
        
        String priority = priorityClassifierService.classifyPriority(
            complaint.getDescription(), 
            complaint.getCategory() != null ? complaint.getCategory().toString() : null
        );
        complaint.setPriority(priority);
        complaint.setAutoAssigned(true);
        complaint.setCreatedAt(LocalDateTime.now());
        complaint.setStatus(Complaint.ComplaintStatus.OPEN);

        Complaint saved = complaintRepository.save(complaint);
        
        // Index in Elasticsearch
        try {
            elasticsearchService.indexComplaint(saved);
            log.info("✅ Complaint indexed in Elasticsearch: " + saved.getId());
        } catch (Exception e) {
            log.warning("⚠️ Failed to index complaint in Elasticsearch: " + e.getMessage());
            // Don't fail the transaction if Elasticsearch is down
        }

        log.info("📝 Filing complaint for user: " + username + " with priority: " + priority);

        // Log user activity
        try {
            User user = getCurrentUser();
            if (user != null) {
                String description = "Filed complaint: " + saved.getDescription();
                if (description.length() > 100) {
                    description = description.substring(0, 100) + "...";
                }
                userActivityService.logActivity(user, "COMPLAINT", description, saved.getId());
            }
        } catch (Exception e) {
            log.warning("Failed to log user activity: " + e.getMessage());
        }

        // Send notification to admin
        try {
            User user = getCurrentUser();
            if (user != null) {
                notificationService.sendGlobalNotification(
                    notificationService.createNotification(
                        null,
                        "New Complaint Filed",
                        "Complaint #" + saved.getId() + " filed by " + user.getName(),
                        "INFO"
                    )
                );
            }
        } catch (Exception e) {
            log.warning("Failed to send notification: " + e.getMessage());
        }

        // Add to blockchain
        try {
            blockchainService.addBlock(saved, "COMPLAINT_CREATED");
        } catch (Exception e) {
            log.warning("Failed to add block to blockchain: " + e.getMessage());
        }

        return saved;
    }

    public List<Complaint> getAllComplaints() {
        if (isAdmin()) {
            return complaintRepository.findAllOrderByCreatedAtDesc();
        }
        String username = currentUsername();
        return complaintRepository.findByResidentUsernameOrderByCreatedAtDesc(username);
    }

    public Page<Complaint> getPaginatedComplaints(Pageable pageable) {
        if (isAdmin()) {
            return complaintRepository.findAll(pageable);
        }
        String username = currentUsername();
        return complaintRepository.findByResidentUsername(username, pageable);
    }

    public Optional<Complaint> getComplaintById(Long id) {
        if (isAdmin()) {
            return complaintRepository.findById(id);
        }
        String username = currentUsername();
        return complaintRepository.findByIdAndResidentUsername(id, username);
    }

    public List<Complaint> getComplaintsByStatus(Complaint.ComplaintStatus status) {
        if (isAdmin()) {
            return complaintRepository.findByStatus(status);
        }
        String username = currentUsername();
        return getAllComplaints().stream().filter(c -> c.getStatus() == status).toList();
    }

    public List<Complaint> getComplaintsByPriority(String priority) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can view complaints by priority");
        }
        return complaintRepository.findByPriority(priority);
    }

    @Transactional
    public Complaint updateStatus(Long id, String status, String resolvedBy, String notes) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can update complaint status");
        }

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        
        Complaint.ComplaintStatus oldStatus = complaint.getStatus();
        Complaint.ComplaintStatus newStatus = Complaint.ComplaintStatus.valueOf(status.toUpperCase());
        complaint.setStatus(newStatus);
        
        if (resolvedBy != null && !resolvedBy.isBlank()) complaint.setResolvedBy(resolvedBy);
        if (notes != null && !notes.isBlank()) complaint.setResolutionNotes(notes);
        
        if (newStatus == Complaint.ComplaintStatus.RESOLVED || newStatus == Complaint.ComplaintStatus.CLOSED) {
            complaint.setResolvedAt(LocalDateTime.now());
        }
        
        Complaint updated = complaintRepository.save(complaint);

        // Update in Elasticsearch
        try {
            elasticsearchService.updateComplaint(updated);
            log.info("✅ Complaint updated in Elasticsearch: " + updated.getId());
        } catch (Exception e) {
            log.warning("⚠️ Failed to update complaint in Elasticsearch: " + e.getMessage());
        }

        // Log user activity
        try {
            User admin = userRepository.findByEmail(currentUsername()).orElse(null);
            if (admin != null) {
                userActivityService.logActivity(
                    admin, 
                    "COMPLAINT", 
                    "Updated complaint #" + id + " status from " + oldStatus + " to " + newStatus, 
                    id
                );
                
                // Audit Log
                auditLogService.logAction(
                    admin,
                    "UPDATE_STATUS",
                    "Updated complaint #" + id + " status from " + oldStatus + " to " + newStatus,
                    "COMPLAINT",
                    id,
                    oldStatus.toString(),
                    newStatus.toString()
                );
            }
        } catch (Exception e) {
            log.warning("Failed to log user activity: " + e.getMessage());
        }

        // Send notification to complaint owner
        try {
            User user = getComplaintUser(id);
            if (user != null) {
                String message = "Your complaint #" + id + " status changed from " + oldStatus + " to " + newStatus;
                notificationService.sendNotification(user,
                    notificationService.createNotification(
                        user,
                        "Complaint Status Updated",
                        message,
                        "INFO"
                    )
                );
            }
            
            notificationService.sendGlobalNotification(
                notificationService.createNotification(
                    null,
                    "Complaint Status Updated",
                    "Complaint #" + id + " status updated to " + newStatus + " by admin",
                    "INFO"
                )
            );
        } catch (Exception e) {
            log.warning("Failed to send notification: " + e.getMessage());
        }

        // Add to blockchain
        try {
            blockchainService.addBlock(updated, "STATUS_UPDATED");
        } catch (Exception e) {
            log.warning("Failed to add block to blockchain: " + e.getMessage());
        }

        return updated;
    }

    @Transactional
    public Complaint updatePriority(Long id, String newPriority) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can manually update complaint priority");
        }
        
        log.info("🔄 Manually updating complaint " + id + " priority to: " + newPriority);
        
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        
        String oldPriority = complaint.getPriority();
        complaint.setPriority(newPriority);
        complaint.setAutoAssigned(false);
        
        Complaint updated = complaintRepository.save(complaint);

        // Update in Elasticsearch
        try {
            elasticsearchService.updateComplaint(updated);
            log.info("✅ Complaint priority updated in Elasticsearch: " + updated.getId());
        } catch (Exception e) {
            log.warning("⚠️ Failed to update complaint priority in Elasticsearch: " + e.getMessage());
        }

        // Log user activity
        try {
            User admin = userRepository.findByEmail(currentUsername()).orElse(null);
            if (admin != null) {
                userActivityService.logActivity(
                    admin, 
                    "COMPLAINT", 
                    "Updated complaint #" + id + " priority from " + oldPriority + " to " + newPriority, 
                    id
                );
                
                // Audit Log
                auditLogService.logAction(
                    admin,
                    "UPDATE_PRIORITY",
                    "Updated complaint #" + id + " priority from " + oldPriority + " to " + newPriority,
                    "COMPLAINT",
                    id,
                    oldPriority,
                    newPriority
                );
            }
        } catch (Exception e) {
            log.warning("Failed to log user activity: " + e.getMessage());
        }

        // Add to blockchain
        try {
            blockchainService.addBlock(updated, "PRIORITY_UPDATED");
        } catch (Exception e) {
            log.warning("Failed to add block to blockchain: " + e.getMessage());
        }

        return updated;
    }

    @Transactional
    public void deleteComplaint(Long id) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can delete complaints");
        }

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));

        // Add to blockchain before deletion
        try {
            blockchainService.addBlock(complaint, "COMPLAINT_DELETED");
        } catch (Exception e) {
            log.warning("Failed to add block to blockchain: " + e.getMessage());
        }

        // Delete from Elasticsearch
        try {
            elasticsearchService.deleteComplaint(id);
            log.info("✅ Complaint deleted from Elasticsearch: " + id);
        } catch (Exception e) {
            log.warning("⚠️ Failed to delete complaint from Elasticsearch: " + e.getMessage());
        }

        // Log user activity
        try {
            User admin = userRepository.findByEmail(currentUsername()).orElse(null);
            if (admin != null) {
                userActivityService.logActivity(
                    admin, 
                    "COMPLAINT", 
                    "Deleted complaint #" + id, 
                    id
                );
                
                // Audit Log
                auditLogService.logAction(
                    admin,
                    "DELETE_COMPLAINT",
                    "Deleted complaint #" + id,
                    "COMPLAINT",
                    id
                );
            }
        } catch (Exception e) {
            log.warning("Failed to log user activity: " + e.getMessage());
        }

        // Send notification before deletion
        try {
            User user = getComplaintUser(id);
            if (user != null) {
                notificationService.sendNotification(user,
                    notificationService.createNotification(
                        user,
                        "Complaint Deleted",
                        "Your complaint #" + id + " has been deleted by admin",
                        "WARNING"
                    )
                );
            }
        } catch (Exception e) {
            log.warning("Failed to send notification: " + e.getMessage());
        }
        
        complaintRepository.deleteById(id);
    }

    public Map<String, Long> getComplaintStats() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can access analytics");
        }

        // Try to get stats from Elasticsearch first for better performance
        try {
            Map<String, Long> esStats = elasticsearchService.getComplaintStatistics();
            if (esStats != null && !esStats.isEmpty()) {
                log.info("✅ Retrieved stats from Elasticsearch");
                return esStats;
            }
        } catch (Exception e) {
            log.warning("⚠️ Failed to get stats from Elasticsearch, falling back to database: " + e.getMessage());
        }

        // Fallback to database
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", complaintRepository.count());
        stats.put("open", complaintRepository.countByStatus(Complaint.ComplaintStatus.OPEN));
        stats.put("inProgress", complaintRepository.countByStatus(Complaint.ComplaintStatus.IN_PROGRESS));
        stats.put("resolved", complaintRepository.countByStatus(Complaint.ComplaintStatus.RESOLVED));
        stats.put("closed", complaintRepository.countByStatus(Complaint.ComplaintStatus.CLOSED));
        return stats;
    }

    public Map<String, Long> getComplaintsByCategory() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can access analytics");
        }

        Map<String, Long> map = new LinkedHashMap<>();
        for (Object[] row : complaintRepository.countByCategory()) {
            map.put(row[0].toString(), (Long) row[1]);
        }
        return map;
    }

    public Map<String, Long> getPriorityStats() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can access analytics");
        }
        
        Map<String, Long> stats = new LinkedHashMap<>();
        List<Object[]> results = complaintRepository.countByPriority();
        for (Object[] result : results) {
            stats.put((String) result[0], (Long) result[1]);
        }
        return stats;
    }

    /**
     * Search complaints using Elasticsearch with full-text search
     */
    public List<Complaint> searchComplaints(String query) {
        if (!isAdmin()) {
            String username = currentUsername();
            // For non-admin users, only search their own complaints
            // This would need to be implemented in the Elasticsearch service
            // with a filter for residentUsername
        }
        
        try {
            var documents = elasticsearchService.fuzzySearch(query);
            // Convert documents back to Complaint entities
            return documents.stream()
                .map(doc -> complaintRepository.findById(doc.getComplaintId()).orElse(null))
                .filter(Objects::nonNull)
                .toList();
        } catch (Exception e) {
            log.warning("⚠️ Failed to search complaints in Elasticsearch: " + e.getMessage());
            // Fallback to database search
            return complaintRepository.findByDescriptionContainingIgnoreCase(query);
        }
    }

    /**
     * Reindex all complaints to Elasticsearch
     */
    @Transactional
    public void reindexAllComplaints() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can reindex complaints");
        }

        log.info("🔄 Starting full reindex of all complaints to Elasticsearch");
        
        List<Complaint> allComplaints = complaintRepository.findAll();
        try {
            elasticsearchService.reindexAll(allComplaints);
            log.info("✅ Successfully reindexed " + allComplaints.size() + " complaints");
        } catch (Exception e) {
            log.severe("❌ Failed to reindex complaints: " + e.getMessage());
            throw new RuntimeException("Reindexing failed", e);
        }
    }

    /**
     * Get complaint analytics from Elasticsearch
     */
    public Map<String, Object> getElasticsearchAnalytics() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can access analytics");
        }

        try {
            Map<String, Object> analytics = new HashMap<>();
            analytics.put("statusDistribution", elasticsearchService.getComplaintStatistics());
            // Add more analytics as needed
            return analytics;
        } catch (Exception e) {
            log.warning("⚠️ Failed to get analytics from Elasticsearch: " + e.getMessage());
            return Map.of("error", "Failed to retrieve analytics");
        }
    }
}