package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "digital_twins")
public class DigitalTwin {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "twin_name", nullable = false)
    private String twinName;
    
    @Column(name = "source_system")
    private String sourceSystem;
    
    @Column(name = "status")
    private String status = "CREATED"; // CREATED, SYNCING, ACTIVE, PAUSED
    
    @Column(name = "sync_frequency")
    private int syncFrequency = 60; // seconds
    
    @Column(name = "last_sync")
    private LocalDateTime lastSync;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "data_snapshot", columnDefinition = "TEXT")
    private String dataSnapshot;
    
    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTwinName() { return twinName; }
    public void setTwinName(String twinName) { this.twinName = twinName; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public int getSyncFrequency() { return syncFrequency; }
    public void setSyncFrequency(int syncFrequency) { this.syncFrequency = syncFrequency; }
    public LocalDateTime getLastSync() { return lastSync; }
    public void setLastSync(LocalDateTime lastSync) { this.lastSync = lastSync; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getDataSnapshot() { return dataSnapshot; }
    public void setDataSnapshot(String dataSnapshot) { this.dataSnapshot = dataSnapshot; }
    public String getConfiguration() { return configuration; }
    public void setConfiguration(String configuration) { this.configuration = configuration; }
}