package com.dinogo.member.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "MemberAccountStatusHistory", schema = "member")
public class MemberAccountStatusHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id") private Integer historyId;
    @Column(name = "member_id", nullable = false) private Integer memberId;
    @Column(name = "previous_status", nullable = false, length = 20) private String previousStatus;
    @Column(name = "new_status", nullable = false, length = 20) private String newStatus;
    @Column(name = "reason", length = 500) private String reason;
    @Column(name = "changed_by") private Integer changedBy;
    @Column(name = "changed_at", nullable = false) private LocalDateTime changedAt;

    public Integer getHistoryId() { return historyId; }
    public Integer getMemberId() { return memberId; }
    public void setMemberId(Integer memberId) { this.memberId = memberId; }
    public String getPreviousStatus() { return previousStatus; }
    public void setPreviousStatus(String previousStatus) { this.previousStatus = previousStatus; }
    public String getNewStatus() { return newStatus; }
    public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Integer getChangedBy() { return changedBy; }
    public void setChangedBy(Integer changedBy) { this.changedBy = changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt) { this.changedAt = changedAt; }
}
