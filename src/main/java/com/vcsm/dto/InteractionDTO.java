package com.vcsm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vcsm.model.Interaction;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;

public class InteractionDTO {

    @JsonProperty("id")
    private Long id;
    @JsonProperty("customerName")
    @NotBlank(message = "Customer name is required")
    private String customerName;
    @JsonProperty("interactionType")
    @NotBlank(message = "Interaction type is required")
    private String interactionType;
    @JsonProperty("summary")
    @NotBlank(message = "Summary is required")
    private String summary;
    @JsonProperty("details")
    private String details;
    @JsonProperty("status")
    private String status;
    @JsonProperty("sentiment")
    private String sentiment;
    @JsonProperty("duration")
    private String duration;
    @JsonProperty("createdAt")
    private LocalDateTime createdAt;
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;
    @JsonProperty("customerEmail")
    private String customerEmail;
    @JsonProperty("customerPhone")
    private String customerPhone;
    @JsonProperty("agentName")
    private String agentName;
    @JsonProperty("category")
    private String category;
    @JsonProperty("outcome")
    private String outcome;
    @JsonProperty("followUpRequired")
    private boolean followUpRequired;

    // Constructors
    public InteractionDTO() {}

    public InteractionDTO(Interaction interaction) {
        this.id = interaction.getId();
        this.customerName = interaction.getCustomerName();
        this.interactionType = interaction.getInteractionType();
        this.summary = interaction.getSummary();
        this.details = interaction.getDetails();
        this.status = interaction.getStatus() != null ? interaction.getStatus().toString() : "COMPLETED";
        this.sentiment = interaction.getSentiment() != null ? interaction.getSentiment().toString() : "NEUTRAL";
        this.duration = interaction.getDuration();
        this.createdAt = interaction.getCreatedAt();
        this.updatedAt = interaction.getUpdatedAt();
        this.customerEmail = interaction.getCustomerEmail();
        this.customerPhone = interaction.getCustomerPhone();
        this.agentName = interaction.getAgentName();
        this.category = interaction.getCategory();
        this.outcome = interaction.getOutcome();
        this.followUpRequired = interaction.isFollowUpRequired();
    }

    // Getters
    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getInteractionType() { return interactionType; }
    public String getSummary() { return summary; }
    public String getDetails() { return details; }
    public String getStatus() { return status; }
    public String getSentiment() { return sentiment; }
    public String getDuration() { return duration; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public String getAgentName() { return agentName; }
    public String getCategory() { return category; }
    public String getOutcome() { return outcome; }
    public boolean isFollowUpRequired() { return followUpRequired; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public void setInteractionType(String interactionType) { this.interactionType = interactionType; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setDetails(String details) { this.details = details; }
    public void setStatus(String status) { this.status = status; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    public void setDuration(String duration) { this.duration = duration; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public void setCategory(String category) { this.category = category; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
    public void setFollowUpRequired(boolean followUpRequired) { this.followUpRequired = followUpRequired; }

    // Helper method to format dates for display
    public String getFormattedCreatedAt() {
        if (createdAt == null) return "";
        return createdAt.format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"));
    }

    // Helper method for sentiment badge color
    public String getSentimentBadgeClass() {
        if (sentiment == null) return "badge-secondary";
        return switch (sentiment) {
            case "POSITIVE" -> "badge-success";
            case "NEGATIVE" -> "badge-danger";
            default -> "badge-secondary";
        };
    }

    // Helper method for status badge color
    public String getStatusBadgeClass() {
        if (status == null) return "badge-secondary";
        return switch (status) {
            case "COMPLETED" -> "badge-success";
            case "IN_PROGRESS" -> "badge-info";
            case "PENDING" -> "badge-warning";
            case "CANCELLED" -> "badge-danger";
            default -> "badge-secondary";
        };
    }
}
