package com.vcsm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CustomerJourneyDTO {

    private String sessionId;
    private String date;
    private String intent;
    private String resolution;
    private long duration; // in minutes

    public CustomerJourneyDTO() {
    }

    public CustomerJourneyDTO(String sessionId, String date, String intent, String resolution, long duration) {
        this.sessionId = sessionId;
        this.date = date;
        this.intent = intent;
        this.resolution = resolution;
        this.duration = duration;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
