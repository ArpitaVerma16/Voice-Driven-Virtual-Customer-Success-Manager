package com.vcsm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "voice_profiles")
public class VoiceProfile {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @NotBlank
    @Column(nullable = false)
    private String name;
    
    @Column(name = "elevenlabs_voice_id")
    private String elevenLabsVoiceId;
    
    @Column(name = "voice_sample_path")
    private String voiceSamplePath;
    
    @Column(name = "is_active")
    private boolean active = false;
    
    @Column(name = "is_default")
    private boolean isDefault = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public VoiceProfile() {}
    
    public VoiceProfile(User user, String name, String voiceSamplePath) {
        this.user = user;
        this.name = name;
        this.voiceSamplePath = voiceSamplePath;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getElevenLabsVoiceId() { return elevenLabsVoiceId; }
    public void setElevenLabsVoiceId(String elevenLabsVoiceId) { this.elevenLabsVoiceId = elevenLabsVoiceId; }
    
    public String getVoiceSamplePath() { return voiceSamplePath; }
    public void setVoiceSamplePath(String voiceSamplePath) { this.voiceSamplePath = voiceSamplePath; }
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}