package com.vcsm.controller;

import com.vcsm.dto.InteractionDTO;
import com.vcsm.model.Interaction;
import com.vcsm.service.InteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Tag(name = "Interactions", description = "Customer Interaction History APIs")
@RestController
@RequestMapping("/api/interactions")
@CrossOrigin(origins = "*")
public class InteractionController {

    @Autowired
    private InteractionService interactionService;

    @Operation(summary = "Create a new interaction", description = "Creates a new customer interaction record")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Interaction created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping
    public ResponseEntity<Interaction> createInteraction(@Valid @RequestBody Interaction interaction) {
        Interaction created = interactionService.createInteraction(interaction);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Get all interactions with pagination")
    @GetMapping
    public ResponseEntity<Page<InteractionDTO>> getAllInteractions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<InteractionDTO> interactions = interactionService.getAllInteractions(pageable);
        return ResponseEntity.ok(interactions);
    }

    @Operation(summary = "Get interaction by ID")
    @GetMapping("/{id}")
    public ResponseEntity<InteractionDTO> getInteractionById(@PathVariable Long id) {
        return interactionService.getInteractionById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Search interactions with filters")
    @GetMapping("/search")
    public ResponseEntity<Page<InteractionDTO>> searchInteractions(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        Sort.Direction direction = Sort.Direction.fromString(sortDirection.toUpperCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<InteractionDTO> results = interactionService.searchInteractions(searchTerm, status, sentiment, category, pageable);
        return ResponseEntity.ok(results);
    }

    @Operation(summary = "Get interactions by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<InteractionDTO>> getByStatus(@PathVariable String status) {
        List<InteractionDTO> interactions = interactionService.getInteractionsByStatus(status);
        return ResponseEntity.ok(interactions);
    }

    @Operation(summary = "Get interactions by sentiment")
    @GetMapping("/sentiment/{sentiment}")
    public ResponseEntity<List<InteractionDTO>> getBySentiment(@PathVariable String sentiment) {
        List<InteractionDTO> interactions = interactionService.getInteractionsBySentiment(sentiment);
        return ResponseEntity.ok(interactions);
    }

    @Operation(summary = "Get recent interactions")
    @GetMapping("/recent")
    public ResponseEntity<List<InteractionDTO>> getRecentInteractions(
            @RequestParam(defaultValue = "10") int limit) {
        List<InteractionDTO> interactions = interactionService.getRecentInteractions(limit);
        return ResponseEntity.ok(interactions);
    }

    @Operation(summary = "Get interactions requiring follow-up")
    @GetMapping("/follow-up")
    public ResponseEntity<Page<InteractionDTO>> getFollowUpInteractions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InteractionDTO> interactions = interactionService.getFollowUpInteractions(pageable);
        return ResponseEntity.ok(interactions);
    }

    @Operation(summary = "Get interactions by date range")
    @GetMapping("/date-range")
    public ResponseEntity<Page<InteractionDTO>> getInteractionsByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            LocalDateTime start = LocalDateTime.parse(startDate, formatter);
            LocalDateTime end = LocalDateTime.parse(endDate, formatter);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<InteractionDTO> interactions = interactionService.getInteractionsByDateRange(start, end, pageable);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(summary = "Get interactions by customer email")
    @GetMapping("/customer/{email}")
    public ResponseEntity<List<InteractionDTO>> getByCustomerEmail(@PathVariable String email) {
        List<InteractionDTO> interactions = interactionService.getInteractionsByCustomerEmail(email);
        return ResponseEntity.ok(interactions);
    }

    @Operation(summary = "Update an interaction")
    @PutMapping("/{id}")
    public ResponseEntity<Interaction> updateInteraction(
            @PathVariable Long id,
            @Valid @RequestBody Interaction interaction) {
        try {
            Interaction updated = interactionService.updateInteraction(id, interaction);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Delete an interaction")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInteraction(@PathVariable Long id) {
        try {
            interactionService.deleteInteraction(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get interaction statistics")
    @GetMapping("/statistics/summary")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        Map<String, Long> stats = interactionService.getInteractionStats();
        return ResponseEntity.ok(stats);
    }
}
