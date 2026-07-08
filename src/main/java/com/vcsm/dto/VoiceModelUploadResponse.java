package com.vcsm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record VoiceModelUploadResponse(
        boolean success,
        String message,
        String modelKey,
        String bucket
) {
}
