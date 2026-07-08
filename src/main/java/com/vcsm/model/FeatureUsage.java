package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "feature_usage")
public class FeatureUsage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "feature_name", nullable = false)
    private String featureName;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "usage_count")
    private int usageCount = 0;
    
    @Column(name = "avg_usage_time")
    private double avgUsageTime;
    
    @Column(name = "success_rate")
    private double successRate;
    
    @Column(name = "user_rating")
    private double userRating;
    
    @Column(name = "last_used")
    private LocalDateTime lastUsed;
    
    @Column(name = "is_active")
    private boolean active = true;
    
    @PrePersist
    protected void onCreate() {
        lastUsed = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    public double getAvgUsageTime() { return avgUsageTime; }
    public void setAvgUsageTime(double avgUsageTime) { this.avgUsageTime = avgUsageTime; }
    public double getSuccessRate() { return successRate; }
    public void setSuccessRate(double successRate) { this.successRate = successRate; }
    public double getUserRating() { return userRating; }
    public void setUserRating(double userRating) { this.userRating = userRating; }
    public LocalDateTime getLastUsed() { return lastUsed; }
    public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}