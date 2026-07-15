package com.vcsm.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class JwtSecretValidator {

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    public void validateJwtSecret() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                "FATAL: JWT_SECRET environment variable is not set. " +
                "Set JWT_SECRET to a strong random string (minimum 32 characters). " +
                "Example: JWT_SECRET=$(openssl rand -base64 32)");
        }

        if (jwtSecret.length() < 32) {
            throw new IllegalStateException(
                "FATAL: JWT_SECRET must be at least 32 characters long for security. " +
                "Current length: " + jwtSecret.length() + " characters. " +
                "Generate a strong secret with: openssl rand -base64 32");
        }

        if (jwtSecret.equalsIgnoreCase("changeme") || jwtSecret.equals("secret") || jwtSecret.equals("defaultsecret")) {
            throw new IllegalStateException(
                "FATAL: JWT_SECRET contains a weak/default value. " +
                "Use a cryptographically secure random string. " +
                "Generate one with: openssl rand -base64 32");
        }
    }
}
