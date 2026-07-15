package com.vcsm.dto;

import java.io.Serializable;

public class InteractionCompletedEvent implements Serializable {

    private String userEmail;
    private String transcript;
    private String intent;
    private boolean success;
    private long responseTime;

    public InteractionCompletedEvent() {}

    public InteractionCompletedEvent(String userEmail, String transcript, String intent, boolean success, long responseTime) {
        this.userEmail = userEmail;
        this.transcript = transcript;
        this.intent = intent;
        this.success = success;
        this.responseTime = responseTime;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
}
