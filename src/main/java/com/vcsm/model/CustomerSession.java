package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "customer_sessions")
public class CustomerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String customerId;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    @Column
    private LocalDateTime endedAt;

    @Column
    private String intent;

    @Column(nullable = false, columnDefinition = "VARCHAR(32) DEFAULT 'unresolved'")
    private String resolutionStatus = "unresolved";

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SessionTurn> turns;

    @Column
    private LocalDateTime archivedAt;

    @Column
    private boolean isArchived = false;

    public CustomerSession() {
        this.startedAt = LocalDateTime.now();
    }

    public CustomerSession(String customerId) {
        this();
        this.customerId = customerId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }

    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }

    public String getResolutionStatus() { return resolutionStatus; }
    public void setResolutionStatus(String resolutionStatus) { this.resolutionStatus = resolutionStatus; }

    public String getTranscript() { return transcript; }
    public void setTranscript(String transcript) { this.transcript = transcript; }

    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }

    public List<SessionTurn> getTurns() { return turns; }
    public void setTurns(List<SessionTurn> turns) { this.turns = turns; }

    public LocalDateTime getArchivedAt() { return archivedAt; }
    public void setArchivedAt(LocalDateTime archivedAt) { this.archivedAt = archivedAt; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public long getDurationSeconds() {
        if (endedAt == null) return -1;
        return java.time.temporal.ChronoUnit.SECONDS.between(startedAt, endedAt);
    }
}
