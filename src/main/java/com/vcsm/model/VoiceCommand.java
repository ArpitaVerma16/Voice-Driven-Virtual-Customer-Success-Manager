package com.vcsm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "voice_commands")
public class VoiceCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 2000)
    @NotBlank
    private String transcript;

    @NotBlank
    private String intent;

    @Column(length = 2000)
    private String response;

    private boolean processed = false;
    
    private LocalDateTime createdAt;

    // Relationship with VoiceFeedback
    @OneToMany(mappedBy = "voiceCommand", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VoiceFeedback> feedbacks = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ---- Getters ----
    public Long getId() { return id; }
    public String getTranscript() { return transcript; }
    public String getIntent() { return intent; }
    public String getResponse() { return response; }
    public boolean isProcessed() { return processed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<VoiceFeedback> getFeedbacks() { return feedbacks; }

    // ---- Setters ----
    public void setId(Long id) { this.id = id; }
    public void setTranscript(String transcript) { this.transcript = transcript; }
    public void setIntent(String intent) { this.intent = intent; }
    public void setResponse(String response) { this.response = response; }
    public void setProcessed(boolean processed) { this.processed = processed; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setFeedbacks(List<VoiceFeedback> feedbacks) { this.feedbacks = feedbacks; }
}