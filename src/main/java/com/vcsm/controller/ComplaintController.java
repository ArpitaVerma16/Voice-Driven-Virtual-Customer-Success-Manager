package com.vcsm.controller;

import com.vcsm.dto.ErrorResponse;
import com.vcsm.model.Complaint;
import com.vcsm.service.ComplaintService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "Complaints", description = "Complaint management APIs")
@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "*")
public class ComplaintController {

    @Autowired
    private ComplaintService complaintService;

    @Operation(summary = "File a new complaint", description = "Creates a new complaint")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Complaint filed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    @PostMapping
    public ResponseEntity<Complaint> file(@Valid @RequestBody Complaint complaint) {
        return ResponseEntity.ok(complaintService.fileComplaint(complaint));
    }

    @Operation(summary = "Get all complaints (with pagination)")
    @GetMapping
    public ResponseEntity<List<Complaint>> getAll() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @Operation(summary = "Get complaints with pagination")
    @GetMapping("/page")
    public ResponseEntity<Page<Complaint>> getPaginatedComplaints(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : 
            Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Complaint> complaints = complaintService.getPaginatedComplaints(pageable);
        
        return ResponseEntity.ok(complaints);
    }

    // ===== SEARCH ENDPOINT =====
    @Operation(summary = "Search complaints with filters")
    @GetMapping("/search")
    public ResponseEntity<Page<Complaint>> searchComplaints(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("asc") ? 
            Sort.by(sortBy).ascending() : 
            Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Complaint> complaints = complaintService.searchComplaints(
            keyword, status, category, priority, startDate, endDate, pageable);
        
        return ResponseEntity.ok(complaints);
    }

    @Operation(summary = "Get complaint by ID")
    @GetMapping("/{id}")
    public ResponseEntity<Complaint> getById(@PathVariable Long id) {
        return complaintService.getComplaintById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Get complaints by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Complaint>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(
                complaintService.getComplaintsByStatus(
                        Complaint.ComplaintStatus.valueOf(status.toUpperCase())
                )
        );
    }

    @Operation(summary = "Get complaints by priority")
    @GetMapping("/priority/{priority}")
    public ResponseEntity<List<Complaint>> getByPriority(@PathVariable String priority) {
        return ResponseEntity.ok(complaintService.getComplaintsByPriority(priority.toUpperCase()));
    }

    @Operation(summary = "Update complaint status")
    @PutMapping("/{id}/status")
    public ResponseEntity<Complaint> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String resolvedBy,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(
                complaintService.updateStatus(id, status, resolvedBy, notes)
        );
    }

    @Operation(summary = "Update complaint priority manually")
    @PutMapping("/{id}/priority")
    public ResponseEntity<Complaint> updatePriority(
            @PathVariable Long id,
            @RequestParam String priority) {
        return ResponseEntity.ok(complaintService.updatePriority(id, priority.toUpperCase()));
    }

    @Operation(summary = "Delete complaint")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        complaintService.deleteComplaint(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get complaint statistics")
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(complaintService.getComplaintStats());
    }

    @Operation(summary = "Get complaints by category")
    @GetMapping("/stats/category")
    public ResponseEntity<Map<String, Long>> statsByCategory() {
        return ResponseEntity.ok(complaintService.getComplaintsByCategory());
    }

    @Operation(summary = "Get priority statistics")
    @GetMapping("/stats/priority")
    public ResponseEntity<Map<String, Long>> getPriorityStats() {
        return ResponseEntity.ok(complaintService.getPriorityStats());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request) {

        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Operation Failed",
                ex.getMessage(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid Request",
            ex.getMessage(),
            "Please check your input and try again.",
            request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}