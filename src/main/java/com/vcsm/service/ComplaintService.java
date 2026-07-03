package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import com.vcsm.specification.ComplaintSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@lombok.RequiredArgsConstructor
public class ComplaintService {

    private static final Logger log = LoggerFactory.getLogger(ComplaintService.class);

    private final ComplaintRepository complaintRepository;

    private final AuditLogService auditLogService;

    private final NotificationService notificationService;

    private final PriorityClassifierService priorityClassifierService;

    private final UserActivityService userActivityService;

    private final UserRepository userRepository;

    private final BlockchainService blockchainService;

    private final EmailService emailService;


    private void safelyExecute(Runnable operation, String description) {
        try {
            operation.run();
        } catch (Exception e) {
            log.error("Failed: " + description, e);
        }
    }

    private void safelyExecute(Runnable operation, String description) {
        try {
            operation.run();
        } catch (Exception e) {
            log.log(Level.SEVERE, "Failed: " + description, e);
        }
    }

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

        log.info("📝 Filing complaint for user: " + username + " with priority: " + priority);

        safelyExecute(() -> {
            User user = getCurrentUser();
            if (user != null) {
                String description = "Filed complaint: " + saved.getDescription();
                if (description.length() > 100) {
                    description = description.substring(0, 100) + "...";
                }
                userActivityService.logActivity(user, "COMPLAINT", description, saved.getId());
            }
        }, "log user activity for complaint filing");
        } catch (Exception e) {
            log.warn("Failed to log user activity: {}", e.getMessage(), e);
        }

        safelyExecute(() -> {
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
        }, "send notification for complaint filing");
        }, "send notification for complaint filing");
        } catch (Exception e) {
            log.warn("Failed to send notification: {}", e.getMessage(), e);
        }

        safelyExecute(() -> blockchainService.addBlock(saved, "COMPLAINT_CREATED"), "add blockchain entry for complaint creation");


        safelyExecute(() -> blockchainService.addBlock(saved, "COMPLAINT_CREATED"), "add blockchain entry for complaint creation");

        return saved;
    }

    public List<Complaint> getAllComplaints() {
        if (isAdmin()) {
            return complaintRepository.findAllOrderByCreatedAtDesc();
        }
        String username = currentUsername();
        return complaintRepository.findByResidentUsernameOrderByCreatedAtDesc(username);
    }


    /**
     * The newest complaints, fetched with a LIMIT query instead of loading
     * the entire complaints table. Permission-aware like getAllComplaints:
     * admins see all complaints, residents see their own.
     */
    public List<Complaint> getRecentComplaints(int limit) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                0, limit, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        return getPaginatedComplaints(pageable).getContent();
    }

    // Pagination method

    public Page<Complaint> getPaginatedComplaints(Pageable pageable) {
        if (isAdmin()) {
            return complaintRepository.findAll(pageable);
        }
        String username = currentUsername();
        return complaintRepository.findByResidentUsername(username, pageable);
    }

    // ===== SEARCH METHOD =====
    public Page<Complaint> searchComplaints(String keyword, String status, String category, 
                                            String priority, LocalDateTime startDate, 
                                            LocalDateTime endDate, Pageable pageable) {
        Specification<Complaint> spec = Specification
            .where(ComplaintSpecification.hasKeyword(keyword))
            .and(ComplaintSpecification.hasStatus(status))
            .and(ComplaintSpecification.hasCategory(category))
            .and(ComplaintSpecification.hasPriority(priority))
            .and(ComplaintSpecification.createdBetween(startDate, endDate));

        if (!isAdmin()) {
            String username = currentUsername();
            spec = spec.and((root, query, cb) -> cb.equal(root.get("residentUsername"), username));
        }

        return complaintRepository.findAll(spec, pageable);
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
        return getAllComplaints().stream().parallel().filter(c -> c.getStatus() == status).toList();
    }

    public List<Complaint> getComplaintsByPriority(String priority) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can view complaints by priority");
        }
        return complaintRepository.findByPriority(priority);
    }

    public List<Complaint> getComplaintsBySubCategory(Complaint.SubCategory subCategory) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can view complaints by sub-category");
        }
        return complaintRepository.findBySubCategory(subCategory);
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
        
        Complaint updated = complaintRepository.save(complaint);
        try {
            User user = getComplaintUser(id);

            if (user != null && user.getEmail() != null && !user.getEmail().isBlank()) {
                String subject = "Complaint Status Updated - #" + id;

                String emailBody =
                        "<p>Hello " + user.getName() + ",</p>" +
                        "<p>Your complaint status has been updated.</p>" +
                        "<p><strong>Complaint ID:</strong> " + id + "</p>" +
                        "<p><strong>Previous Status:</strong> " + oldStatus + "</p>" +
                        "<p><strong>New Status:</strong> " + newStatus + "</p>" +
                        "<p><strong>Resolution Notes:</strong> " +
                        (notes != null && !notes.isBlank() ? notes : "No resolution notes provided.") +
                        "</p>" +
                        "<p>Regards,<br>VCSM Team</p>";

                emailService.sendSimpleEmail(user.getEmail(), subject, emailBody);
            }
        } catch (Exception e) {
            log.warning("Failed to send complaint status update email: " + e.getMessage());
        }
        // Log user activity
        try {
        safelyExecute(() -> {
            User admin = userRepository.findByEmail(currentUsername()).orElse(null);
            if (admin != null) {
                userActivityService.logActivity(
                    admin, 
                    "COMPLAINT", 
                    "Updated complaint #" + id + " status from " + oldStatus + " to " + newStatus, 
                    id
                );
                
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
        }, "log user activity and audit for status update");
        }, "log user activity and audit for status update");
        } catch (Exception e) {
            log.warn("Failed to log user activity: {}", e.getMessage(), e);
        }

        safelyExecute(() -> {
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
        }, "send notifications for status update");
        } catch (Exception e) {
            log.warn("Failed to send notification: {}", e.getMessage(), e);
        }

        safelyExecute(() -> blockchainService.addBlock(updated, "STATUS_UPDATED"), "add blockchain entry for status update");


        safelyExecute(() -> blockchainService.addBlock(updated, "STATUS_UPDATED"), "add blockchain entry for status update");

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

        safelyExecute(() -> {
            User admin = userRepository.findByEmail(currentUsername()).orElse(null);
            if (admin != null) {
                userActivityService.logActivity(
                    admin, 
                    "COMPLAINT", 
                    "Updated complaint #" + id + " priority from " + oldPriority + " to " + newPriority, 
                    id
                );
                
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
        }, "log user activity and audit for priority update");

        safelyExecute(() -> blockchainService.addBlock(updated, "PRIORITY_UPDATED"), "add blockchain entry for priority update");
        } catch (Exception e) {
            log.warn("Failed to log user activity: {}", e.getMessage(), e);
        }

        safelyExecute(() -> blockchainService.addBlock(updated, "PRIORITY_UPDATED"), "add blockchain entry for priority update");

        return updated;
    }

    @Transactional
    public void deleteComplaint(Long id) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can delete complaints");
        }

        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));

        safelyExecute(() -> blockchainService.addBlock(complaint, "COMPLAINT_DELETED"), "add blockchain entry for complaint deletion");


        safelyExecute(() -> {
            User admin = userRepository.findByEmail(currentUsername()).orElse(null);
            if (admin != null) {
                userActivityService.logActivity(
                    admin, 
                    "COMPLAINT", 
                    "Deleted complaint #" + id, 
                    id
                );
                
                auditLogService.logAction(
                    admin,
                    "DELETE_COMPLAINT",
                    "Deleted complaint #" + id,
                    "COMPLAINT",
                    id
                );
            }
        }, "log user activity and audit for complaint deletion");
        }, "log user activity and audit for complaint deletion");
        } catch (Exception e) {
            log.warn("Failed to log user activity: {}", e.getMessage(), e);
        }

        safelyExecute(() -> {
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
        }, "send notification for complaint deletion");
        }, "send notification for complaint deletion");
        } catch (Exception e) {
            log.warn("Failed to send notification: {}", e.getMessage(), e);
        }
        
        complaintRepository.deleteById(id);
    }

    public Map<String, Long> getComplaintStats() {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can access analytics");
        }

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
}