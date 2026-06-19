package com.vcsm.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "interactions")
public class Interaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotBlank(message = "Interaction type is required")
    private String interactionType; // VOICE_CALL, TEXT_CHAT, EMAIL, etc.

    @NotBlank(message = "Summary is required")
    @Column(length = 500)
    private String summary;

    @Column(length = 2000)
    private String details;

    @Enumerated(EnumType.STRING)
    private InteractionStatus status; // COMPLETED, PENDING, IN_PROGRESS

    @Enumerated(EnumType.STRING)
    private SentimentType sentiment; // POSITIVE, NEUTRAL, NEGATIVE

    private String duration; // Duration of interaction (for calls)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private String customerEmail;
    private String customerPhone;

    // Agent/Staff information
    private String agentName;

    // Topic/Category
    private String category; // COMPLAINT, INQUIRY, FEEDBACK, etc.

    // For search functionality
    @Column(length = 500)
    private String searchKeywords;

    // Outcome
    private String outcome; // RESOLVED, ESCALATED, PENDING, etc.

    // Follow-up flag
    private boolean followUpRequired;

    @Column(name = "customer_username")
    private String customerUsername; // For access control

    @PrePersist
    protected void onCreate() {
        if (status == null) status = InteractionStatus.COMPLETED;
        if (sentiment == null) sentiment = SentimentType.NEUTRAL;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        generateSearchKeywords();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        generateSearchKeywords();
    }

    private void generateSearchKeywords() {
        StringBuilder keywords = new StringBuilder();
        if (customerName != null) keywords.append(customerName).append(" ");
        if (interactionType != null) keywords.append(interactionType).append(" ");
        if (category != null) keywords.append(category).append(" ");
        if (summary != null) keywords.append(summary).append(" ");
        this.searchKeywords = keywords.toString().toLowerCase();
    }

    // ---- Getters ----
    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getInteractionType() { return interactionType; }
    public String getSummary() { return summary; }
    public String getDetails() { return details; }
    public InteractionStatus getStatus() { return status; }
    public SentimentType getSentiment() { return sentiment; }
    public String getDuration() { return duration; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public String getAgentName() { return agentName; }
    public String getCategory() { return category; }
    public String getSearchKeywords() { return searchKeywords; }
    public String getOutcome() { return outcome; }
    public boolean isFollowUpRequired() { return followUpRequired; }
    public String getCustomerUsername() { return customerUsername; }

    // ---- Setters ----
    public void setId(Long id) { this.id = id; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setInteractionType(String interactionType) { this.interactionType = interactionType; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setDetails(String details) { this.details = details; }
    public void setStatus(InteractionStatus status) { this.status = status; }
    public void setSentiment(SentimentType sentiment) { this.sentiment = sentiment; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public void setCategory(String category) { this.category = category; }
    public void setSearchKeywords(String searchKeywords) { this.searchKeywords = searchKeywords; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public void setFollowUpRequired(boolean followUpRequired) { this.followUpRequired = followUpRequired; }
    public void setCustomerUsername(String customerUsername) { this.customerUsername = customerUsername; }

    // ---- Enums ----
    public enum InteractionStatus {
        COMPLETED, PENDING, IN_PROGRESS, CANCELLED
    }

    public enum SentimentType {
        POSITIVE, NEUTRAL, NEGATIVE
    }
}
