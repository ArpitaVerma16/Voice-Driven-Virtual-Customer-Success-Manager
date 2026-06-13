package com.vcsm.model;

public class VoiceVerificationRequest {
    private Long userId;
    private String voiceSample;
    private String text;
    
    public VoiceVerificationRequest() {}
    
    public VoiceVerificationRequest(Long userId, String voiceSample, String text) {
        this.userId = userId;
        this.voiceSample = voiceSample;
        this.text = text;
    }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getVoiceSample() { return voiceSample; }
    public void setVoiceSample(String voiceSample) { this.voiceSample = voiceSample; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}