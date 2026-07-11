package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

@Entity
@Table(name = "session_turns")
public class SessionTurn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private CustomerSession session;

    @Column(nullable = false)
    private Integer turnIndex;

    @Column(nullable = false, length = 16)
    private String speaker; // "customer" or "system"

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime timestamp;

    public SessionTurn() {
        this.timestamp = LocalDateTime.now();
    }

    public SessionTurn(CustomerSession session, Integer turnIndex, String speaker, String content) {
        this();
        this.session = session;
        this.turnIndex = turnIndex;
        this.speaker = speaker;
        this.content = content;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public CustomerSession getSession() { return session; }
    public void setSession(CustomerSession session) { this.session = session; }

    public Integer getTurnIndex() { return turnIndex; }
    public void setTurnIndex(Integer turnIndex) { this.turnIndex = turnIndex; }

    public String getSpeaker() { return speaker; }
    public void setSpeaker(String speaker) { this.speaker = speaker; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}