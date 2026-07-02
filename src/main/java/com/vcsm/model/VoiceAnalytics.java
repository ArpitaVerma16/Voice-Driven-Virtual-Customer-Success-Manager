package com.vcsm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voice_analytics")
public class VoiceAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    private String commandText;
    private String intent;
    private boolean success;

    @Min(0)
    private long responseTime; // in milliseconds

    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    

    // Constructors

    public VoiceAnalytics() {}
    
    public VoiceAnalytics(User user, String commandText, String intent, boolean success, long responseTime) {
        this.user = user;
        this.commandText = commandText;
        this.intent = intent;
        this.success = success;
        this.responseTime = responseTime;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCommandText() { return commandText; }
    public void setCommandText(String commandText) { this.commandText = commandText; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public long getResponseTime() { return responseTime; }
    public void setResponseTime(long responseTime) { this.responseTime = responseTime; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}