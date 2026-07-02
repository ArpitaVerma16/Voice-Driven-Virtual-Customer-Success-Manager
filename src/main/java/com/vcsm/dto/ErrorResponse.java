package com.vcsm.dto;

import java.time.LocalDateTime;

public class ErrorResponse {

    @JsonProperty("status")
    private int status;
    @JsonProperty("error")
    private String error;
    @JsonProperty("message")
    private String message;
    @JsonProperty("userMessage")
    private String userMessage;
    @JsonProperty("path")
    private String path;
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor used by VoiceController
    public ErrorResponse(int status, String error, String message) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.userMessage = message;
        this.path = "";
        this.timestamp = LocalDateTime.now();
    }

    // Full constructor
    public ErrorResponse(
            int status,
            String error,
            String message,
            String userMessage,
            String path) {

        this.status = status;
        this.error = error;
        this.message = message;
        this.userMessage = userMessage;
        this.path = path;
        this.timestamp = LocalDateTime.now();
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

