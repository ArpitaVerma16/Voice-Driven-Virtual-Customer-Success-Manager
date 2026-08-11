# Voice Command API Validation Guide

## Overview
This document describes the request validation flow for the Voice Command API, including transcript validation, authentication, language detection, intent processing, and sentiment analysis. This guide serves as the blueprint for the Java implementation.

## 1. Validation Flow

### Step 1: Empty Transcript Validation
- **Check**: Ensure the received transcript is not null or empty.
- **Action**: If empty, return `400 Bad Request`.
- **Reason**: Prevents processing of failed speech-to-text conversions.

### Step 2: Transcript Length Limits
- **Constraint**: Maximum length of 500 characters.
- **Action**: If exceeded, return `413 Payload Too Large`.
- **Reason**: Ensures system performance and prevents buffer overflows.

### Step 3: Authentication Checks
- **Requirement**: Valid API Key or Bearer Token in the header.
- **Action**: If missing or invalid, return `401 Unauthorized`.

### Step 4: Language Detection
- **Process**: Detect language of the transcript.
- **Constraint**: Only English (`en-US`) is currently supported.
- **Action**: If unsupported language, return `400 Bad Request` with message "Unsupported Language".

### Step 5: Command Processing & Intent
- **Process**: Parse the transcript to identify the intent (e.g., "check balance", "reset password").
- **Validation**: Verify the intent matches a known use case.
- **Action**: If unknown intent, return `400 Bad Request`.

### Step 6: Sentiment Analysis
- **Process**: Analyze user sentiment (Positive, Neutral, Negative).
- **Usage**: Used for logging and routing angry customers to human agents.
- **Validation**: Must return a valid sentiment score.

## 2. Java Implementation Examples

### Input Sanitization
```java
public static String sanitizeInput(String input) {
    if (input == null || input.trim().isEmpty()) {
        throw new IllegalArgumentException("Transcript cannot be empty");
    }
    // Allow only letters, numbers, and spaces
    return input.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
}   
