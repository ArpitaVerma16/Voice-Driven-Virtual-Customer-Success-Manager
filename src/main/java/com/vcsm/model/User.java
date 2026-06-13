package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String name;
    
    private String password;
    
    @Column(name = "preferred_language")
private String preferredLanguage = "en";

    @Column(name = "is_voice_enrolled")
    private boolean isVoiceEnrolled = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Complaint> complaints = new ArrayList<>();
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private VoicePrint voicePrint;
    
    public User() {}
    
    public User(String email, String name, String password) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public boolean isVoiceEnrolled() { return isVoiceEnrolled; }
    public void setVoiceEnrolled(boolean voiceEnrolled) { isVoiceEnrolled = voiceEnrolled; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public List<Complaint> getComplaints() { return complaints; }
    public void setComplaints(List<Complaint> complaints) { this.complaints = complaints; }
    
    public VoicePrint getVoicePrint() { return voicePrint; }
    public void setVoicePrint(VoicePrint voicePrint) { this.voicePrint = voicePrint; }

    public String getPreferredLanguage() { return preferredLanguage; }
public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
}