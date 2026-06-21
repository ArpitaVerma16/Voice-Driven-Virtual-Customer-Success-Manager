package com.vcsm.security.dto;

public class AuthRequest {
    private String email;
    private String password;

    public AuthRequest() {}

    public AuthRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return email; }
    public void setUsername(String username) { this.email = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}