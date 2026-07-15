package com.vcsm.model;

import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.*;

import com.fasterxml.jackson.annotation.JsonInclude;

public class AgentRequest {
    @NotBlank
    private String userQuery;
    @NotNull
    private Long userId;
    private List<String> intents;
    private Map<String, Object> context;

    public AgentRequest() {}

    public AgentRequest(String userQuery, Long userId) {
        this.userQuery = userQuery;
        this.userId = userId;
    }

    // Getters and Setters
    public String getUserQuery() { return userQuery; }
    public void setUserQuery(String userQuery) { this.userQuery = userQuery; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public List<String> getIntents() { return intents; }
    public void setIntents(List<String> intents) { this.intents = intents; }

    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}