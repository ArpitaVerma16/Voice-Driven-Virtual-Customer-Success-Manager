package com.vcsm.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.vcsm.model.Interaction;

@Repository
public interface InteractionRepository extends JpaRepository<Interaction, Long> {

    // Find interactions by customer username (for access control)
    List<Interaction> findByCustomerUsername(String customerUsername);

    Page<Interaction> findByCustomerUsername(String customerUsername, Pageable pageable);

    // Find interactions by status
    List<Interaction> findByStatus(Interaction.InteractionStatus status);

    Page<Interaction> findByStatus(Interaction.InteractionStatus status, Pageable pageable);

    // Find interactions by sentiment
    List<Interaction> findBySentiment(Interaction.SentimentType sentiment);

    Page<Interaction> findBySentiment(Interaction.SentimentType sentiment, Pageable pageable);

    // Find interactions by category
    List<Interaction> findByCategory(String category);

    Page<Interaction> findByCategory(String category, Pageable pageable);

    // Find interactions by interaction type
    List<Interaction> findByInteractionType(String interactionType);

    Page<Interaction> findByInteractionType(String interactionType, Pageable pageable);

    // Find interactions by customer name
    List<Interaction> findByCustomerNameContainingIgnoreCase(String customerName);

    Page<Interaction> findByCustomerNameContainingIgnoreCase(String customerName, Pageable pageable);

    // Find interactions by date range
    List<Interaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    Page<Interaction> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    // Find interactions by follow-up required
    List<Interaction> findByFollowUpRequired(boolean followUpRequired);

    Page<Interaction> findByFollowUpRequired(boolean followUpRequired, Pageable pageable);

    // Custom search query - search across multiple fields
    @Query("SELECT i FROM Interaction i WHERE " +
           "LOWER(i.customerName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.details) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.category) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.interactionType) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<Interaction> searchInteractions(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Search with additional filters
    @Query("SELECT i FROM Interaction i WHERE " +
           "(LOWER(i.customerName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(i.summary) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:sentiment IS NULL OR i.sentiment = :sentiment) AND " +
           "(:category IS NULL OR i.category = :category)")
    Page<Interaction> searchWithFilters(
            @Param("searchTerm") String searchTerm,
            @Param("status") Interaction.InteractionStatus status,
            @Param("sentiment") Interaction.SentimentType sentiment,
            @Param("category") String category,
            Pageable pageable
    );

    // Find recent interactions
    @Query(value = "SELECT * FROM interactions ORDER BY created_at DESC LIMIT :limit", nativeQuery = true)
    List<Interaction> findRecentInteractions(@Param("limit") int limit);

    // Count interactions by status
    long countByStatus(Interaction.InteractionStatus status);

    // Count interactions by sentiment
    long countBySentiment(Interaction.SentimentType sentiment);

    // Get all interactions for a customer
    List<Interaction> findByCustomerEmailIgnoreCase(String customerEmail);

    // Pagination for all interactions
    Page<Interaction> findAll(Pageable pageable);

    // Find interactions requiring follow-up
    List<Interaction> findByFollowUpRequiredAndCustomerUsername(boolean followUpRequired, String customerUsername);

    Page<Interaction> findByFollowUpRequiredAndCustomerUsername(boolean followUpRequired, String customerUsername, Pageable pageable);
}
