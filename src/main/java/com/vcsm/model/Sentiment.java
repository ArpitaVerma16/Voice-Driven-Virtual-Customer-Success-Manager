package com.vcsm.model;

public enum Sentiment {
    POSITIVE("Satisfied, happy, resolved"),
    NEUTRAL("Standard inquiry, no strong emotion"),
    NEGATIVE("Dissatisfied, frustrated"),
    DISTRESSED("Angry, urgent escalation needed");

    private final String description;

    Sentiment(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
