package com.vcsm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "complaints")
public class Complaint {

    private static final String DEFAULT_PRIORITY = "MEDIUM";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Resident name is required")
    @Column(nullable = false, length = 100)
    private String residentName;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 1000)
    @Size(max = 500)
    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private ComplaintStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private ComplaintCategory category;

    @Column(length = 20)
    private String apartmentNumber;

    @Column(length = 100)
    @Email
    private String contactEmail;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(length = 100)
    private String resolvedBy;

    // Auth ownership: residents can only view/manage their own complaints
    @Column(length = 100)
    private String residentUsername;

    @Column(length = 500)
    private String resolutionNotes;

    @Column(name = "priority", nullable = false)
    private String priority = DEFAULT_PRIORITY;

    @Column(name = "auto_assigned", nullable = false)
    private boolean autoAssigned = true;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @PrePersist
    protected void onCreate() {
        if (status == null) {
            status = ComplaintStatus.OPEN;
        }

        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ---------------- Getters ----------------

    public Long getId() {
        return id;
    }

    public String getResidentName() {
        return residentName;
    }

    public String getDescription() {
        return description;
    }

    public ComplaintStatus getStatus() {
        return status;
    }

    public ComplaintCategory getCategory() {
        return category;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public String getResidentUsername() {
        return residentUsername;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public String getPriority() {
        return priority;
    }

    public boolean isAutoAssigned() {
        return autoAssigned;
    }

    public User getUser() {
        return user;
    }

    // ---------------- Setters ----------------

    public void setId(Long id) {
        this.id = id;
    }

    public void setResidentName(String residentName) {
        this.residentName = residentName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(ComplaintStatus status) {
        this.status = status;
    }

    public void setCategory(ComplaintCategory category) {
        this.category = category;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setResolvedBy(String resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public void setResidentUsername(String residentUsername) {
        this.residentUsername = residentUsername;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setAutoAssigned(boolean autoAssigned) {
        this.autoAssigned = autoAssigned;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // ---------------- Helper Methods ----------------

    public boolean isOpen() {
        return status == ComplaintStatus.OPEN
                || status == ComplaintStatus.IN_PROGRESS;
    }

    public boolean isResolved() {
        return status == ComplaintStatus.RESOLVED
                || status == ComplaintStatus.CLOSED;
    }

    // ---------------- Enums ----------------

    public enum ComplaintStatus {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        CLOSED
    }

    public enum ComplaintCategory {
        NOISE,
        MAINTENANCE,
        SECURITY,
        CLEANLINESS,
        PARKING,
        UTILITIES,
        OTHER
    }

    public enum PriorityLevel {
        CRITICAL,
        HIGH,
        MEDIUM,
        LOW
    }

    @Override
    public String toString() {
        return "Complaint{" +
                "id=" + id +
                ", status=" + status +
                ", category=" + category +
                ", priority='" + priority + '\'' +
                ", residentUsername='" + residentUsername + '\'' +
                '}';
    }
}