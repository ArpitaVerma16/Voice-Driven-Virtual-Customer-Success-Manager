package com.vcsm.service;

import com.vcsm.dto.ComplaintCommentDTO;
import com.vcsm.model.Complaint;
import com.vcsm.model.ComplaintComment;
import com.vcsm.model.User;
import com.vcsm.repository.ComplaintCommentRepository;
import com.vcsm.repository.ComplaintRepository;
import com.vcsm.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
@lombok.RequiredArgsConstructor
public class ComplaintCommentService {

    private final ComplaintCommentRepository complaintCommentRepository;
    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String currentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private boolean canAccessComplaint(Complaint complaint) {
        if (isAdmin()) return true;
        String username = currentUsername();
        return username != null && username.equals(complaint.getResidentUsername());
    }

    @Transactional(readOnly = true)
    public List<ComplaintCommentDTO> getCommentsForComplaint(Long complaintId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));

        if (!canAccessComplaint(complaint)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to view comments for this complaint.");
        }

        return complaintCommentRepository.findByComplaintIdOrderByCreatedAtAsc(complaintId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ComplaintCommentDTO addComment(Long complaintId, String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment content cannot be empty.");
        }
        
        if (content.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment content exceeds maximum length of 1000 characters.");
        }

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));

        if (!canAccessComplaint(complaint)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to comment on this complaint.");
        }

        String username = currentUsername();
        if (username == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        ComplaintComment comment = new ComplaintComment(complaint, user, content.trim());
        ComplaintComment savedComment = complaintCommentRepository.save(comment);

        // Notify the other party
        try {
            if (isAdmin() && !user.getEmail().equals(complaint.getResidentUsername())) {
                // Admin commented, notify resident
                User resident = userRepository.findByEmail(complaint.getResidentUsername()).orElse(null);
                if (resident != null) {
                    notificationService.sendNotification(resident,
                            notificationService.createNotification(
                                    resident,
                                    "New Comment on Complaint #" + complaint.getId(),
                                    "An admin replied: " + truncateContent(content),
                                    "INFO"
                            )
                    );
                }
            } else if (!isAdmin()) {
                // Resident commented, notify global admins
                notificationService.sendGlobalNotification(
                        notificationService.createNotification(
                                null,
                                "New Resident Comment on Complaint #" + complaint.getId(),
                                user.getName() + " says: " + truncateContent(content),
                                "INFO"
                        )
                );
            }
        } catch (Exception e) {
            // Log silently, don't fail comment creation
        }

        return convertToDTO(savedComment);
    }

    private String truncateContent(String content) {
        if (content.length() > 50) return content.substring(0, 47) + "...";
        return content;
    }

    private ComplaintCommentDTO convertToDTO(ComplaintComment comment) {
        // Since we don't have a direct "isAdmin" field on User, we can infer it
        // based on whether their email matches the complaint's resident username
        // (If not, they are likely the admin). Or we can check their roles if we have them.
        boolean commentByAdmin = false;
        
        // simple heuristic: if the commenter is not the complaint owner, they are acting as admin here.
        if (comment.getUser() != null && comment.getComplaint() != null) {
            if (!comment.getUser().getEmail().equals(comment.getComplaint().getResidentUsername())) {
                commentByAdmin = true;
            }
        }
        
        return new ComplaintCommentDTO(
                comment.getId(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUser() != null ? comment.getUser().getName() : "Unknown",
                comment.getUser() != null ? comment.getUser().getEmail() : "Unknown",
                commentByAdmin
        );
    }
}
