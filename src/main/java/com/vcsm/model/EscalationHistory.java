package com.vcsm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "escalation_history")
public class EscalationHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Complaint associated with this escalation
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id", nullable = false)
    private Complaint complaint;

    /**
     * Previous complaint priority
     */
    @Column(name = "old_priority")
    private String oldPriority;

    /**
     * Updated complaint priority
     */
    @Column(name = "new_priority")
    private String newPriority;

    /**
     * Previous complaint status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "old_status")
    private Complaint.ComplaintStatus oldStatus;

    /**
     * Updated complaint status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status")
    private Complaint.ComplaintStatus newStatus;

    /**
     * Reason for escalation
     */
    @Column(length = 500)
    private String reason;

    /**
     * Escalation level (1,2,3...)
     */
    @Column(name = "escalation_level")
    private Integer escalationLevel;

    /**
     * Timestamp
     */
    @Column(name = "escalated_at")
    private LocalDateTime escalatedAt;

    @PrePersist
    public void onCreate() {
        if (escalatedAt == null) {
            escalatedAt = LocalDateTime.now();
        }
    }

    public EscalationHistory() {
    }

    public EscalationHistory(
            Complaint complaint,
            String oldPriority,
            String newPriority,
            Complaint.ComplaintStatus oldStatus,
            Complaint.ComplaintStatus newStatus,
            String reason,
            Integer escalationLevel) {

        this.complaint = complaint;
        this.oldPriority = oldPriority;
        this.newPriority = newPriority;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.escalationLevel = escalationLevel;
    }

    public Long getId() {
        return id;
    }

    public Complaint getComplaint() {
        return complaint;
    }

    public void setComplaint(Complaint complaint) {
        this.complaint = complaint;
    }

    public String getOldPriority() {
        return oldPriority;
    }

    public void setOldPriority(String oldPriority) {
        this.oldPriority = oldPriority;
    }

    public String getNewPriority() {
        return newPriority;
    }

    public void setNewPriority(String newPriority) {
        this.newPriority = newPriority;
    }

    public Complaint.ComplaintStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(Complaint.ComplaintStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public Complaint.ComplaintStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(Complaint.ComplaintStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getEscalationLevel() {
        return escalationLevel;
    }

    public void setEscalationLevel(Integer escalationLevel) {
        this.escalationLevel = escalationLevel;
    }

    public LocalDateTime getEscalatedAt() {
        return escalatedAt;
    }

    public void setEscalatedAt(LocalDateTime escalatedAt) {
        this.escalatedAt = escalatedAt;
    }
}