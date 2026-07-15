package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "voice_prints")
public class VoicePrint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Column(name = "voice_features", columnDefinition = "TEXT")
    private String voiceFeatures;

    @Column(name = "sample_duration")
    private double sampleDuration;

    @Column(name = "language")
    private String language;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public VoicePrint() {
        this.createdAt = LocalDateTime.now();
    }

    public VoicePrint(User user, String voiceFeatures, double sampleDuration) {
        this.user = user;
        this.voiceFeatures = voiceFeatures;
        this.sampleDuration = sampleDuration;
        this.createdAt = LocalDateTime.now();
    }

    public VoicePrint(User user, String voiceFeatures, double sampleDuration, String language) {
        this.user = user;
        this.voiceFeatures = voiceFeatures;
        this.sampleDuration = sampleDuration;
        this.language = language;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getVoiceFeatures() { return voiceFeatures; }
    public void setVoiceFeatures(String voiceFeatures) { this.voiceFeatures = voiceFeatures; }

    public double getSampleDuration() { return sampleDuration; }
    public void setSampleDuration(double sampleDuration) { this.sampleDuration = sampleDuration; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}