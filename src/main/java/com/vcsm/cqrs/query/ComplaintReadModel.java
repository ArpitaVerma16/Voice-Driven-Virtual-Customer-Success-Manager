package com.vcsm.cqrs.query;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaint_read_model")
public class ComplaintReadModel {

    @Id
    private Long id;
    private String residentName;
    private String description;
    private String category;
    private String status;
    private String priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private double avgResolutionTime;
    private String resolvedBy;

    // Constructors
    public ComplaintReadModel() {}

    public ComplaintReadModel(Long id, String residentName, String description, String category,
                              String status, String priority, LocalDateTime createdAt,
                              LocalDateTime updatedAt, String resolvedBy) {
        this.id = id;
        this.residentName = residentName;
        this.description = description;
        this.category = category;
        this.status = status;
        this.priority = priority;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedBy = resolvedBy;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getResidentName() { return residentName; }
    public void setResidentName(String residentName) { this.residentName = residentName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public double getAvgResolutionTime() { return avgResolutionTime; }
    public void setAvgResolutionTime(double avgResolutionTime) { this.avgResolutionTime = avgResolutionTime; }

    public String getResolvedBy() { return resolvedBy; }
    public void setResolvedBy(String resolvedBy) { this.resolvedBy = resolvedBy; }
}