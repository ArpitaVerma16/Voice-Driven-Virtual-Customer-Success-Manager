package com.vcsm.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET is_deleted = true, deleted_at = CURRENT_TIMESTAMP WHERE id=?")
@Where(clause = "is_deleted = false")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank
    @Email
    @Column(unique = true, nullable = false)
    private String email;
    
    @NotBlank
    @Column(nullable = false)
    private String name;
    
    @Size(min = 8, max = 100)
    private String password;
    
    @Column(name = "preferred_language")
    private String preferredLanguage = "en";
    
    @Column(name = "is_voice_enrolled")
    private boolean isVoiceEnrolled = false;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // Profile Fields
    @Pattern(regexp = "\\+?[0-9]{10,15}")
    @Column(name = "phone")
    private String phone;
    
    @Column(name = "profile_image")
    private String profileImage;
    
    @Column(name = "last_active")
    private LocalDateTime lastActive;
    
    // Notification Preferences
    @Column(name = "email_notifications")
    private boolean emailNotifications = true;
    
    @Column(name = "sms_notifications")
    private boolean smsNotifications = false;
    
    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "dissatisfaction_score")
    private double dissatisfactionScore = 0.0;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Complaint> complaints = new ArrayList<>();
    
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private VoicePrint voicePrint;
    
    @Column(name = "is_deleted")
    private boolean isDeleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    // Constructors
    public User() {}
    
    public User(String email, String name) {
        this.email = email;
        this.name = name;
        this.createdAt = LocalDateTime.now();
        this.emailNotifications = true;
        this.smsNotifications = false;
    }
    
    // ---- Getters ----
    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public boolean isVoiceEnrolled() { return isVoiceEnrolled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getPhone() { return phone; }
    public String getProfileImage() { return profileImage; }
    public LocalDateTime getLastActive() { return lastActive; }
    public boolean isEmailNotifications() { return emailNotifications; }
    public boolean isSmsNotifications() { return smsNotifications; }
    public String getPhoneNumber() { return phoneNumber; }
    public List<Complaint> getComplaints() { return complaints; }
    public VoicePrint getVoicePrint() { return voicePrint; }
    
    public boolean isDeleted() { return isDeleted; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    
    // ---- Setters ----
    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setName(String name) { this.name = name; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public void setVoiceEnrolled(boolean voiceEnrolled) { isVoiceEnrolled = voiceEnrolled; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }
    public void setLastActive(LocalDateTime lastActive) { this.lastActive = lastActive; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    public void setSmsNotifications(boolean smsNotifications) { this.smsNotifications = smsNotifications; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setComplaints(List<Complaint> complaints) { this.complaints = complaints; }
    public void setVoicePrint(VoicePrint voicePrint) { this.voicePrint = voicePrint; }

    public double getDissatisfactionScore() { return dissatisfactionScore; }
    public void setDissatisfactionScore(double dissatisfactionScore) { this.dissatisfactionScore = dissatisfactionScore; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}