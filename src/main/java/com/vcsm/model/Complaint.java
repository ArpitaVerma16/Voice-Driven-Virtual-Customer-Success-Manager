package com.vcsm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Resident name is required")
    private String residentName;

    @NotBlank(message = "Description is required")
    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    private ComplaintStatus status;

    @Enumerated(EnumType.STRING)
    private ComplaintCategory category;

    private String apartmentNumber;
    private String contactEmail;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String resolvedBy;

    // Auth ownership: residents can only view/manage their own complaints
    private String residentUsername;

    @Column(length = 500)
    private String resolutionNotes;

    // Priority Auto-Assign Fields
    @Column(name = "priority")
    private String priority = "MEDIUM";

    @Column(name = "auto_assigned")
    private boolean autoAssigned = true;

    @PrePersist
    protected void onCreate() {
        if (status == null) status = ComplaintStatus.OPEN;
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---- Getters ----
    public Long getId() { return id; }
    public String getResidentName() { return residentName; }
    public String getDescription() { return description; }
    public ComplaintStatus getStatus() { return status; }
    public ComplaintCategory getCategory() { return category; }
    public String getApartmentNumber() { return apartmentNumber; }
    public String getContactEmail() { return contactEmail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getResolvedBy() { return resolvedBy; }
    public String getResolutionNotes() { return resolutionNotes; }
    public String getResidentUsername() { return residentUsername; }
    public String getPriority() { return priority; }
    public boolean isAutoAssigned() { return autoAssigned; }

    // ---- Setters ----
    public void setId(Long id) { this.id = id; }
    public void setResidentName(String residentName) { this.residentName = residentName; }
    public void setDescription(String description) { this.description = description; }
    public void setStatus(ComplaintStatus status) { this.status = status; }
    public void setCategory(ComplaintCategory category) { this.category = category; }
    public void setApartmentNumber(String apartmentNumber) { this.apartmentNumber = apartmentNumber; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
    public void setResidentUsername(String residentUsername) { this.residentUsername = residentUsername; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setAutoAssigned(boolean autoAssigned) { this.autoAssigned = autoAssigned; }

    // ---- Enums ----
    public enum ComplaintStatus { 
        OPEN, IN_PROGRESS, RESOLVED, CLOSED 
    }
    
    public enum ComplaintCategory { 
        NOISE, MAINTENANCE, SECURITY, CLEANLINESS, PARKING, UTILITIES, OTHER 
    }
    public enum PriorityLevel {
    CRITICAL, HIGH, MEDIUM, LOW
}
}