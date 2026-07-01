package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "model_versions")
public class ModelVersion {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "model_name", nullable = false)
    private String modelName;
    
    @Column(name = "version", nullable = false)
    private String version;
    
    @Column(name = "accuracy")
    private double accuracy;
    
    @Column(name = "precision_score")
    private double precisionScore;
    
    @Column(name = "recall_score")
    private double recallScore;
    
    @Column(name = "f1_score")
    private double f1Score;
    
    @Column(name = "training_data_size")
    private int trainingDataSize;
    
    @Column(name = "training_time")
    private long trainingTime;
    
    @Column(name = "is_active")
    private boolean isActive = false;
    
    @Column(name = "is_deployed")
    private boolean isDeployed = false;
    
    @Column(name = "deployment_url")
    private String deploymentUrl;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "deployed_at")
    private LocalDateTime deployedAt;
    
    @Column(name = "performance_metrics")
    private String performanceMetrics; // JSON string
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
    public double getPrecisionScore() { return precisionScore; }
    public void setPrecisionScore(double precisionScore) { this.precisionScore = precisionScore; }
    public double getRecallScore() { return recallScore; }
    public void setRecallScore(double recallScore) { this.recallScore = recallScore; }
    public double getF1Score() { return f1Score; }
    public void setF1Score(double f1Score) { this.f1Score = f1Score; }
    public int getTrainingDataSize() { return trainingDataSize; }
    public void setTrainingDataSize(int trainingDataSize) { this.trainingDataSize = trainingDataSize; }
    public long getTrainingTime() { return trainingTime; }
    public void setTrainingTime(long trainingTime) { this.trainingTime = trainingTime; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public boolean isDeployed() { return isDeployed; }
    public void setDeployed(boolean deployed) { isDeployed = deployed; }
    public String getDeploymentUrl() { return deploymentUrl; }
    public void setDeploymentUrl(String deploymentUrl) { this.deploymentUrl = deploymentUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getDeployedAt() { return deployedAt; }
    public void setDeployedAt(LocalDateTime deployedAt) { this.deployedAt = deployedAt; }
    public String getPerformanceMetrics() { return performanceMetrics; }
    public void setPerformanceMetrics(String performanceMetrics) { this.performanceMetrics = performanceMetrics; }
}