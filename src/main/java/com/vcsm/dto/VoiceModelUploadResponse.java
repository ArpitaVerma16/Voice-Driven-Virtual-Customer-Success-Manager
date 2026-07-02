package com.vcsm.dto;

public record VoiceModelUploadResponse(
        boolean success,
        String message,
        String modelKey,
        String bucket
) {
}

