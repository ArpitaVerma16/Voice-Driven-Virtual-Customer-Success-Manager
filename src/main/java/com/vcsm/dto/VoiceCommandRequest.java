package com.vcsm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public class VoiceCommandRequest {

    @NotBlank(message = "Transcript must not be blank")
    @Size(max = 500, message = "Transcript must not exceed 500 characters")
    @Pattern(
        regexp = "^[\\p{L}\\p{N}\\s.,?!'-]+$",
        message = "Transcript contains invalid characters"
    )
    private String transcript;

    public String getTranscript() {
        return transcript;
    }

    public void setTranscript(String transcript) {
        this.transcript = transcript;
    }
}