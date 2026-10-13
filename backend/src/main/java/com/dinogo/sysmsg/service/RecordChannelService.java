package com.dinogo.sysmsg.service;

import java.time.LocalDateTime;

import com.dinogo.sysmsg.entity.RecordChannelEntity;
import com.dinogo.sysmsg.dto.external.MemberAuthResponse;

public interface RecordChannelService {
    RecordChannelEntity createEmailChannel(
            Integer sendId, Integer recordId,
            MemberAuthResponse member, String msgFunction,
            boolean sellerRecipient);

    RecordChannelEntity markEmailSent(Integer recordChannelId, String providerMessageId, LocalDateTime sentAt);
    RecordChannelEntity markEmailFailed(
            Integer recordChannelId,
            String errorMessage,
            String failureCode,
            LocalDateTime attemptedAt,
            LocalDateTime nextRetryAt,
            int maxAttempts);
}
