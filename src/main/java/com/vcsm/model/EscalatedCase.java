package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "escalated_cases")
public class EscalatedCase {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne
    @JoinColumn(name = "sentiment_id")
    private SentimentAnalysis sentimentAnalysis;
    
    private String priority = "HIGH";
    
    private boolean adminNotified = false;
    
    private boolean resolved = false;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(length = 500)
    private String notes;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public EscalatedCase() {
        this.createdAt = LocalDateTime.now();
    }
    
    public EscalatedCase(SentimentAnalysis sentimentAnalysis) {
        this.sentimentAnalysis = sentimentAnalysis;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public SentimentAnalysis getSentimentAnalysis() { return sentimentAnalysis; }
    public void setSentimentAnalysis(SentimentAnalysis sentimentAnalysis) { this.sentimentAnalysis = sentimentAnalysis; }
    
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    
    public boolean isAdminNotified() { return adminNotified; }
    public void setAdminNotified(boolean adminNotified) { this.adminNotified = adminNotified; }
    
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
    
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}