package com.vcsm.model;

public class AdaptiveVoiceSettings {
    private double stability;
    private double similarityBoost;
    private double style;
    private boolean useSpeakerBoost;
    private double speedFactor;

    public AdaptiveVoiceSettings() {}

    public AdaptiveVoiceSettings(double stability, double similarityBoost, double style, boolean useSpeakerBoost, double speedFactor) {
        this.stability = stability;
        this.similarityBoost = similarityBoost;
        this.style = style;
        this.useSpeakerBoost = useSpeakerBoost;
        this.speedFactor = speedFactor;
    }

    public double getStability() {
        return stability;
    }

    public void setStability(double stability) {
        this.stability = stability;
    }

    public double getSimilarityBoost() {
        return similarityBoost;
    }

    public void setSimilarityBoost(double similarityBoost) {
        this.similarityBoost = similarityBoost;
    }

    public double getStyle() {
        return style;
    }

    public void setStyle(double style) {
        this.style = style;
    }

    public boolean isUseSpeakerBoost() {
        return useSpeakerBoost;
    }

    public void setUseSpeakerBoost(boolean useSpeakerBoost) {
        this.useSpeakerBoost = useSpeakerBoost;
    }

    public double getSpeedFactor() {
        return speedFactor;
    }

    public void setSpeedFactor(double speedFactor) {
        this.speedFactor = speedFactor;
    }
}
