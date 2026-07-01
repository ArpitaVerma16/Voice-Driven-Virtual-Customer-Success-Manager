package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ivr_flow_configs")
public class IvrFlowConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String flowJson;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public IvrFlowConfig() {}

    public IvrFlowConfig(String flowJson, boolean isActive) {
        this.flowJson = flowJson;
        this.isActive = isActive;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFlowJson() { return flowJson; }
    public void setFlowJson(String flowJson) { this.flowJson = flowJson; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
