package com.vcsm.service;

import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
    private PriorityClassifierService priorityClassifierService;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private UserRepository userRepository;

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
        
        return saved;
    }

    public List<Complaint> getAllComplaints() {
        if (isAdmin()) {
            return complaintRepository.findAllOrderByCreatedAtDesc();
        }
        String username = currentUsername();
        return complaintRepository.findByResidentUsernameOrderByCreatedAtDesc(username);
    }


    // Pagination method

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
            }
        } catch (Exception e) {
            log.warning("Failed to log user activity: " + e.getMessage());
        }
        
        return updated;
    }

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
        

        try {
            User admin = userRepository.findByEmail(currentUsername()).orElse(null);
            if (admin != null) {
                userActivityService.logActivity(
                    admin, 
                    "COMPLAINT", 
                    "Updated complaint #" + id + " priority from " + oldPriority + " to " + newPriority, 
                    id
                );
            }
        } catch (Exception e) {
            log.warning("Failed to log user activity: " + e.getMessage());
        }
        
        return updated;
    }

    public void deleteComplaint(Long id) {
        if (!isAdmin()) {
            throw new AccessDeniedException("Only admins can delete complaints");
        }
        

        try {
            User admin = userRepository.findByEmail(currentUsername()).orElse(null);
            if (admin != null) {
                userActivityService.logActivity(
                    admin, 
                    "COMPLAINT", 
                    "Deleted complaint #" + id, 
                    id
                );
            }
        } catch (Exception e) {
            log.warning("Failed to log user activity: " + e.getMessage());
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