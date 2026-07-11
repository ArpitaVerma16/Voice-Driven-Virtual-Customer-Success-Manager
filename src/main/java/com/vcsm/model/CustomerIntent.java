package com.vcsm.model;

public enum CustomerIntent {
    BILLING_INQUIRY("Billing & Invoice Queries"),
    TECHNICAL_SUPPORT("Technical Issues & Troubleshooting"),
    ACCOUNT_MANAGEMENT("Account Settings & Profile"),
    CANCELLATION_REQUEST("Service Cancellation"),
    FEATURE_REQUEST("Feature Requests & Feedback"),
    GENERAL_INQUIRY("General Questions");

    private final String description;

    CustomerIntent(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
