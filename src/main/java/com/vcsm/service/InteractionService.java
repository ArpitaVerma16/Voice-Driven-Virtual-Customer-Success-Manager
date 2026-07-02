package com.vcsm.service;

import com.vcsm.dto.InteractionDTO;
import com.vcsm.model.Interaction;
import com.vcsm.repository.InteractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InteractionService {

    @Autowired
    private InteractionRepository interactionRepository;

    // Get current user from security context
    private String getCurrentUsername() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    // Check if current user is admin
    private boolean isAdmin() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Create a new interaction
     */
    public Interaction createInteraction(Interaction interaction) {
        String username = getCurrentUsername();
        if (username == null) throw new RuntimeException("Unauthorized");

        interaction.setCustomerUsername(username);
        if (interaction.getStatus() == null) {
            interaction.setStatus(Interaction.InteractionStatus.COMPLETED);
        }
        if (interaction.getSentiment() == null) {
            interaction.setSentiment(Interaction.SentimentType.NEUTRAL);
        }

        return interactionRepository.save(interaction);
    }

    /**
     * Get interaction by ID
     */
    public Optional<InteractionDTO> getInteractionById(Long id) {
        Optional<Interaction> interaction = interactionRepository.findById(id);
        if (interaction.isPresent()) {
            // Check access: user can only view their own interactions or admin can view all
            Interaction i = interaction.get();
            String currentUsername = getCurrentUsername();
            if (isAdmin() || (currentUsername != null && currentUsername.equals(i.getCustomerUsername()))) {
                return Optional.of(new InteractionDTO(i));
            }
        }
        return Optional.empty();
    }

    /**
     * Get all interactions for current user with pagination
     */
    public Page<InteractionDTO> getAllInteractions(Pageable pageable) {
        String username = getCurrentUsername();
        if (username == null) throw new RuntimeException("Unauthorized");

        Page<Interaction> interactions;
        if (isAdmin()) {
            interactions = interactionRepository.findAll(pageable);
        } else {
            interactions = interactionRepository.findByCustomerUsername(username, pageable);
        }

        return interactions.map(InteractionDTO::new);
    }

    /**
     * Search interactions with optional filters
     */
    public Page<InteractionDTO> searchInteractions(String searchTerm, String status, String sentiment,
                                                   String category, Pageable pageable) {
        String username = getCurrentUsername();
        if (username == null) throw new RuntimeException("Unauthorized");

        Page<Interaction> results;

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            Interaction.InteractionStatus statusEnum = null;
            Interaction.SentimentType sentimentEnum = null;

            try {
                if (status != null && !status.isEmpty()) {
                    statusEnum = Interaction.InteractionStatus.valueOf(status.toUpperCase());
                }
                if (sentiment != null && !sentiment.isEmpty()) {
                    sentimentEnum = Interaction.SentimentType.valueOf(sentiment.toUpperCase());
                }
            } catch (IllegalArgumentException e) {
                // Invalid enum value, will be ignored
            }

            results = interactionRepository.searchWithFilters(
                    searchTerm,
                    statusEnum,
                    sentimentEnum,
                    category != null && !category.isEmpty() ? category : null,
                    pageable
            );
        } else {
            // No search term, just apply filters
            results = applyFilters(status, sentiment, category, pageable);
        }

        // Filter by username if not admin
        if (!isAdmin()) {
            List<InteractionDTO> filtered = results.stream()
                    .map(InteractionDTO::new)
                    .filter(i -> username.equals(i.getCustomerName())) // You might need to adjust this
                    .collect(Collectors.toList());
            return new PageImpl<>(filtered, pageable, results.getTotalElements());
        }

        return results.map(InteractionDTO::new);
    }

    /**
     * Apply filters to interactions
     */
    private Page<Interaction> applyFilters(String status, String sentiment, String category, Pageable pageable) {
        String username = getCurrentUsername();

        try {
            if (status != null && !status.isEmpty()) {
                Interaction.InteractionStatus statusEnum = Interaction.InteractionStatus.valueOf(status.toUpperCase());
                Interaction.InteractionStatus statusEnum = Interaction.InteractionStatus.valueOf(status.toUpperCase());
        // Removed dead if/else branch - both paths executed same code
                    return interactionRepository.findByStatus(statusEnum, pageable);
                }
            } else if (sentiment != null && !sentiment.isEmpty()) {
                Interaction.SentimentType sentimentEnum = Interaction.SentimentType.valueOf(sentiment.toUpperCase());
                Interaction.SentimentType sentimentEnum = Interaction.SentimentType.valueOf(sentiment.toUpperCase());
        // Removed dead if/else branch - both paths executed same code
                    return interactionRepository.findBySentiment(sentimentEnum, pageable);
                }
            } else if (category != null && !category.isEmpty()) {
            } else if (category != null && !category.isEmpty()) {
        // Removed dead if/else branch - both paths executed same code
                    return interactionRepository.findByCategory(category, pageable);
                }
            } else {
                if (isAdmin()) {
                    return interactionRepository.findAll(pageable);
                } else {
                    return interactionRepository.findByCustomerUsername(username, pageable);
                }
            }
        } catch (IllegalArgumentException e) {
            // Invalid enum, return all
            if (isAdmin()) {
                return interactionRepository.findAll(pageable);
            } else {
                return interactionRepository.findByCustomerUsername(username, pageable);
            }
        }
    }

    /**
     * Get interactions by status
     */
    public List<InteractionDTO> getInteractionsByStatus(String status) {
        try {
            Interaction.InteractionStatus statusEnum = Interaction.InteractionStatus.valueOf(status.toUpperCase());
            return interactionRepository.findByStatus(statusEnum).stream()
                    .map(InteractionDTO::new)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Get interactions by sentiment
     */
    public List<InteractionDTO> getInteractionsBySentiment(String sentiment) {
        try {
            Interaction.SentimentType sentimentEnum = Interaction.SentimentType.valueOf(sentiment.toUpperCase());
            return interactionRepository.findBySentiment(sentimentEnum).stream()
                    .map(InteractionDTO::new)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Get recent interactions
     */
    public List<InteractionDTO> getRecentInteractions(int limit) {
        return interactionRepository.findRecentInteractions(limit).stream()
                .map(InteractionDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Update an interaction
     */
    public Interaction updateInteraction(Long id, Interaction updatedInteraction) {
        Optional<Interaction> existing = interactionRepository.findById(id);
        if (existing.isEmpty()) {
            throw new RuntimeException("Interaction not found");
        }

        Interaction interaction = existing.get();

        // Check access
        String currentUsername = getCurrentUsername();
        if (!isAdmin() && !currentUsername.equals(interaction.getCustomerUsername())) {
            throw new RuntimeException("Unauthorized");
        }

        // Update fields
        if (updatedInteraction.getCustomerName() != null) {
            interaction.setCustomerName(updatedInteraction.getCustomerName());
        }
        if (updatedInteraction.getSummary() != null) {
            interaction.setSummary(updatedInteraction.getSummary());
        }
        if (updatedInteraction.getDetails() != null) {
            interaction.setDetails(updatedInteraction.getDetails());
        }
        if (updatedInteraction.getStatus() != null) {
            interaction.setStatus(updatedInteraction.getStatus());
        }
        if (updatedInteraction.getSentiment() != null) {
            interaction.setSentiment(updatedInteraction.getSentiment());
        }
        if (updatedInteraction.getOutcome() != null) {
            interaction.setOutcome(updatedInteraction.getOutcome());
        }
        interaction.setFollowUpRequired(updatedInteraction.isFollowUpRequired());

        return interactionRepository.save(interaction);
    }

    /**
     * Delete an interaction
     */
    public void deleteInteraction(Long id) {
        Optional<Interaction> interaction = interactionRepository.findById(id);
        if (interaction.isEmpty()) {
            throw new RuntimeException("Interaction not found");
        }

        // Check access
        String currentUsername = getCurrentUsername();
        if (!isAdmin() && !currentUsername.equals(interaction.get().getCustomerUsername())) {
            throw new RuntimeException("Unauthorized");
        }

        interactionRepository.deleteById(id);
    }

    /**
     * Get interaction statistics
     */
    public Map<String, Long> getInteractionStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("total", interactionRepository.count());
        stats.put("completed", interactionRepository.countByStatus(Interaction.InteractionStatus.COMPLETED));
        stats.put("pending", interactionRepository.countByStatus(Interaction.InteractionStatus.PENDING));
        stats.put("inProgress", interactionRepository.countByStatus(Interaction.InteractionStatus.IN_PROGRESS));
        stats.put("positive", interactionRepository.countBySentiment(Interaction.SentimentType.POSITIVE));
        stats.put("neutral", interactionRepository.countBySentiment(Interaction.SentimentType.NEUTRAL));
        stats.put("negative", interactionRepository.countBySentiment(Interaction.SentimentType.NEGATIVE));
        return stats;
    }

    /**
     * Get interactions requiring follow-up
     */
    public Page<InteractionDTO> getFollowUpInteractions(Pageable pageable) {
        String username = getCurrentUsername();
        Page<Interaction> followups;
        if (isAdmin()) {
            followups = interactionRepository.findByFollowUpRequired(true, pageable);
        } else {
            followups = interactionRepository.findByFollowUpRequiredAndCustomerUsername(true, username, pageable);
        }
        return followups.map(InteractionDTO::new);
    }

    /**
     * Get interactions by date range
     */
    public Page<InteractionDTO> getInteractionsByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        Page<Interaction> interactions = interactionRepository.findByCreatedAtBetween(startDate, endDate, pageable);
        return interactions.map(InteractionDTO::new);
    }

    /**
     * Get interactions by customer email
     */
    public List<InteractionDTO> getInteractionsByCustomerEmail(String email) {
        return interactionRepository.findByCustomerEmailIgnoreCase(email).stream()
                .map(InteractionDTO::new)
                .collect(Collectors.toList());
    }
}
