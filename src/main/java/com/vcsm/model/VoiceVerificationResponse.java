package com.vcsm.model;

public class VoiceVerificationResponse {
    private boolean verified;
    private double confidence;
    private String message;
    private Long userId;
    private String userName;
    
    public VoiceVerificationResponse() {}
    
    public VoiceVerificationResponse(boolean verified, double confidence, String message) {
        this.verified = verified;
        this.confidence = confidence;
        this.message = message;
    }
    
    public VoiceVerificationResponse(boolean verified, double confidence, String message, Long userId, String userName) {
        this.verified = verified;
        this.confidence = confidence;
        this.message = message;
        this.userId = userId;
        this.userName = userName;
    }
    
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}