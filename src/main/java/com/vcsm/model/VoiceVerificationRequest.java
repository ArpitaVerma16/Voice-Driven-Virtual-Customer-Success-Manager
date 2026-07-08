package com.vcsm.model;

import com.fasterxml.jackson.annotation.JsonInclude;

public class VoiceVerificationRequest {
    private Long userId;
    private String voiceSample;
    private String text;
    private String language;
    
    public VoiceVerificationRequest() {}
    
    public VoiceVerificationRequest(Long userId, String voiceSample, String text) {
        this.userId = userId;
        this.voiceSample = voiceSample;
        this.text = text;
        this.language = "en";
    }

    public VoiceVerificationRequest(Long userId, String voiceSample, String text, String language) {
        this.userId = userId;
        this.voiceSample = voiceSample;
        this.text = text;
        this.language = language;
    }
    
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    
    public String getVoiceSample() { return voiceSample; }
    public void setVoiceSample(String voiceSample) { this.voiceSample = voiceSample; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}