package com.dinogo.sysmsg.entity;

import java.time.LocalDateTime;
import org.hibernate.annotations.Nationalized;
import jakarta.persistence.*;

@Entity
@Table(name = "record_channel", schema = "sysmsg",
       uniqueConstraints = @UniqueConstraint(
           name = "UX_sysmsg_record_channel_type",
           columnNames = {"record_id", "channel_type"}))
public class RecordChannelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_channel_id")
    private Integer recordChannelId;

    @Column(name = "send_id", nullable = false, updatable = false)
    private Integer sendId;

    @Column(name = "record_id", nullable = false, updatable = false)
    private Integer recordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 10, nullable = false, updatable = false)
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 20, nullable = false, updatable = false)
    private NotificationType notificationType;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Nationalized
    @Column(name = "provider_message_id", length = 200)
    private String providerMessageId;
    @Nationalized
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Nationalized
    @Column(name = "failure_code", length = 50)
    private String failureCode;

    @Column(name = "dead_lettered_at")
    private LocalDateTime deadLetteredAt;

    protected RecordChannelEntity() {}

    public RecordChannelEntity(Integer sendId, Integer recordId,
            ChannelType channelType, NotificationType notificationType) {
        this.sendId = sendId;
        this.recordId = recordId;
        this.channelType = channelType;
        this.notificationType = notificationType;
    }

    public Integer getRecordChannelId() { return recordChannelId; }
    public Integer getSendId() { return sendId; }
    public Integer getRecordId() { return recordId; }
    public ChannelType getChannelType() { return channelType; }
    public NotificationType getNotificationType() { return notificationType; }
    public LocalDateTime getSentAt() { return sentAt; }
    public String getProviderMessageId() { return providerMessageId; }
    public String getErrorMessage() { return errorMessage; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getLastAttemptAt() { return lastAttemptAt; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public String getFailureCode() { return failureCode; }
    public LocalDateTime getDeadLetteredAt() { return deadLetteredAt; }

    public void startAttempt(LocalDateTime attemptedAt) {
        if (attemptedAt == null) {
            throw new IllegalArgumentException("嘗試時間不可為 null");
        }
        if (isSent() || isDeadLettered()) {
            throw new IllegalStateException("已完成或死信 Channel 不可再嘗試");
        }
        this.attemptCount++;
        this.lastAttemptAt = attemptedAt;
        this.sentAt = null;
        this.providerMessageId = null;
        this.errorMessage = null;
        this.failureCode = null;
        this.nextRetryAt = null;
    }

    /** Spring Boot 完成送交 Email Provider 時呼叫；時間由應用程式明確控制。 */
    public void markSent(String providerMessageId, LocalDateTime sentAt) {
        if (sentAt == null) {
            throw new IllegalArgumentException("sentAt 不可為 null");
        }
        if (providerMessageId == null || providerMessageId.isBlank()) {
            throw new IllegalArgumentException("發送成功時必須保存 providerMessageId");
        }
        this.sentAt = sentAt;
        this.providerMessageId = providerMessageId;
        this.errorMessage = null;
        if (this.attemptCount < 1 || this.lastAttemptAt == null) {
            throw new IllegalStateException("必須先記錄發送嘗試");
        }
        this.nextRetryAt = null;
        this.failureCode = null;
        this.deadLetteredAt = null;
    }

    public void markFailed(
            String errorMessage,
            String failureCode,
            LocalDateTime attemptedAt,
            LocalDateTime retryAt,
            int maxAttempts) {
        if (errorMessage == null || errorMessage.isBlank()) {
            throw new IllegalArgumentException("寄送失敗時必須提供 errorMessage");
        }
        if (failureCode == null || failureCode.isBlank()) {
            throw new IllegalArgumentException("寄送失敗時必須提供 failureCode");
        }
        if (attemptedAt == null || maxAttempts < 1) {
            throw new IllegalArgumentException("嘗試時間與最大次數不可無效");
        }
        this.sentAt = null;
        this.providerMessageId = null;
        this.errorMessage = errorMessage;
        this.failureCode = failureCode;
        if (this.attemptCount < 1 || this.lastAttemptAt == null) {
            throw new IllegalStateException("必須先記錄發送嘗試");
        }
        if (this.attemptCount >= maxAttempts || retryAt == null) {
            this.nextRetryAt = null;
            this.deadLetteredAt = attemptedAt;
        } else {
            this.nextRetryAt = retryAt;
            this.deadLetteredAt = null;
        }
    }

    public boolean isPending() {
        return attemptCount == 0
                && sentAt == null
                && errorMessage == null
                && deadLetteredAt == null;
    }
    public boolean isAttemptInProgress() {
        return attemptCount > 0
                && lastAttemptAt != null
                && sentAt == null
                && providerMessageId == null
                && errorMessage == null
                && failureCode == null
                && nextRetryAt == null
                && deadLetteredAt == null;
    }
    public boolean isSent() { return sentAt != null && errorMessage == null; }
    public boolean isRetryableAt(LocalDateTime now) {
        return sentAt == null
                && providerMessageId == null
                && errorMessage != null
                && nextRetryAt != null
                && deadLetteredAt == null
                && !nextRetryAt.isAfter(now);
    }
    public boolean isFailed() { return sentAt == null && errorMessage != null; }
    public boolean isDeadLettered() { return deadLetteredAt != null; }
}
