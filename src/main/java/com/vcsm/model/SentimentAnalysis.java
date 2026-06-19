package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sentiment_analyses")
public class SentimentAnalysis {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @ManyToOne
    @JoinColumn(name = "complaint_id")
    private Complaint complaint;
    
    @Column(nullable = false)
    private String sentiment;  // VERY_POSITIVE, POSITIVE, NEUTRAL, NEGATIVE, VERY_NEGATIVE
    
    private double confidence;
    
    @Column(length = 1000)
    private String transcribedText;
    
    private boolean wasEscalated = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public SentimentAnalysis() {
        this.createdAt = LocalDateTime.now();
    }
    
    public SentimentAnalysis(User user, String sentiment, double confidence, String transcribedText) {
        this.user = user;
        this.sentiment = sentiment;
        this.confidence = confidence;
        this.transcribedText = transcribedText;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public Complaint getComplaint() { return complaint; }
    public void setComplaint(Complaint complaint) { this.complaint = complaint; }
    
    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public String getTranscribedText() { return transcribedText; }
    public void setTranscribedText(String transcribedText) { this.transcribedText = transcribedText; }
    
    public boolean isWasEscalated() { return wasEscalated; }
    public void setWasEscalated(boolean wasEscalated) { this.wasEscalated = wasEscalated; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}