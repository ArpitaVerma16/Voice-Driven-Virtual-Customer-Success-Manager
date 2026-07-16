package com.vcsm.controller.graphql;

import com.vcsm.model.Complaint;
import com.vcsm.model.User;
import com.vcsm.service.ComplaintService;
import com.vcsm.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ComplaintResolver {

    private final ComplaintService complaintService;
    private final UserService userService;

    // ========== QUERIES ==========
    
    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ComplaintPage complaints(
        @Argument ComplaintFilterInput filter,
        @Argument PaginationInput pagination
    ) {
        // Implementation with filter and pagination
        return complaintService.getComplaintPage(filter, pagination);
    }

    @QueryMapping
    public Complaint complaint(@Argument Long id) {
        return complaintService.getComplaintById(id)
            .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public ComplaintPage myComplaints(@Argument PaginationInput pagination) {
        return complaintService.getMyComplaints(pagination);
    }

    @QueryMapping
    public ComplaintStats complaintStats() {
        Map<String, Long> stats = complaintService.getComplaintStats();
        return new ComplaintStats(
            stats.get("total"),
            stats.get("open"),
            stats.get("inProgress"),
            stats.get("resolved"),
            stats.get("closed")
        );
    }

    // ========== MUTATIONS ==========

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    public Complaint createComplaint(@Argument CreateComplaintInput input) {
        Complaint complaint = new Complaint();
        complaint.setDescription(input.getDescription());
        complaint.setCategory(Complaint.ComplaintCategory.valueOf(input.getCategory().name()));
        complaint.setApartmentNumber(input.getApartmentNumber());
        complaint.setContactEmail(input.getContactEmail());
        return complaintService.fileComplaint(complaint);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Complaint updateComplaintStatus(@Argument UpdateStatusInput input) {
        return complaintService.updateStatus(
            input.getComplaintId(),
            input.getStatus().name(),
            null,
            input.getNotes()
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Complaint updateComplaintPriority(@Argument Long id, @Argument String priority) {
        return complaintService.updatePriority(id, priority);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteComplaint(@Argument Long id) {
        complaintService.deleteComplaint(id);
        return true;
    }

    // ========== SCHEMA RESOLVERS (Field-level) ==========

    @SchemaMapping(typeName = "Complaint", field = "comments")
    public List<Comment> getComments(Complaint complaint) {
        return complaintService.getComments(complaint.getId());
    }

    @SchemaMapping(typeName = "Complaint", field = "residentUsername")
    public String getResidentUsername(Complaint complaint) {
        return complaint.getResidentUsername();
    }

    // ========== DTOs for GraphQL ==========

    public record ComplaintStats(
        long total,
        long open,
        long inProgress,
        long resolved,
        long closed
    ) {}

    public record ComplaintPage(
        List<Complaint> content,
        PageInfo pageInfo
    ) {}

    public record PageInfo(
        long totalElements,
        int totalPages,
        int pageNumber,
        int pageSize,
        boolean hasNext,
        boolean hasPrevious
    ) {}

    public record ComplaintFilterInput(
        Complaint.ComplaintStatus status,
        Complaint.ComplaintCategory category,
        String priority,
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        String search
    ) {}

    public record PaginationInput(
        int page,
        int size,
        String sortBy,
        SortOrder sortOrder
    ) {}

    public enum SortOrder {
        ASC, DESC
    }

    public record CreateComplaintInput(
        String description,
        Complaint.ComplaintCategory category,
        String apartmentNumber,
        String contactEmail
    ) {}

    public record UpdateStatusInput(
        Long complaintId,
        Complaint.ComplaintStatus status,
        String notes
    ) {}
}