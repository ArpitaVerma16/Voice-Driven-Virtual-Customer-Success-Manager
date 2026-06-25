package com.vcsm.model;

import java.time.LocalDateTime;

public class TriageRequest {
    private Long complaintId;
    private String description;
    private String category;
    private String severity;
    private Double confidence;
    private String assignedTo;
    private String eta;
    private LocalDateTime createdAt;

    // Constructors, Getters, Setters
    public TriageRequest() {}

    public TriageRequest(Long complaintId, String description) {
        this.complaintId = complaintId;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    public Long getComplaintId() { return complaintId; }
    public void setComplaintId(Long complaintId) { this.complaintId = complaintId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Double getConfidence() { return confidence; }
    public void setConfidence(Double confidence) { this.confidence = confidence; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getEta() { return eta; }
    public void setEta(String eta) { this.eta = eta; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}