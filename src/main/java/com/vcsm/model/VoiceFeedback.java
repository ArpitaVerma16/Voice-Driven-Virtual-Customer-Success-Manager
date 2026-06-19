package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "voice_feedback")
public class VoiceFeedback {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "command_id")
    private VoiceCommand voiceCommand;
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
    
    @Column(name = "feedback", nullable = false)
    private String feedback; // 'UP' or 'DOWN'
    
    @Column(length = 500)
    private String comment;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Constructors
    public VoiceFeedback() {}
    
    public VoiceFeedback(VoiceCommand voiceCommand, User user, String feedback, String comment) {
        this.voiceCommand = voiceCommand;
        this.user = user;
        this.feedback = feedback;
        this.comment = comment;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public VoiceCommand getVoiceCommand() { return voiceCommand; }
    public void setVoiceCommand(VoiceCommand voiceCommand) { this.voiceCommand = voiceCommand; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}