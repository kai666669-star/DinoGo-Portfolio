package com.dinogo.sysmsg.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.dinogo.sysmsg.entity.ChannelType;
import com.dinogo.sysmsg.entity.NotificationType;
import com.dinogo.sysmsg.entity.RecordChannelEntity;
import com.dinogo.sysmsg.dto.external.MemberAuthResponse;
import com.dinogo.sysmsg.repository.RecordChannelRepository;
import com.dinogo.sysmsg.service.RecordChannelService;
import com.dinogo.sysmsg.service.EmailSubscriptionPolicy;
import java.util.NoSuchElementException;
import com.dinogo.sysmsg.exception.SysmsgConflictException;

@Service
public class RecordChannelServiceImpl implements RecordChannelService {
    private final RecordChannelRepository channelRepository;

    public RecordChannelServiceImpl(RecordChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public RecordChannelEntity createEmailChannel(
            Integer sendId, Integer recordId,
            MemberAuthResponse member, String msgFunction,
            boolean sellerRecipient) {
        NotificationType type = NotificationType.resolve(msgFunction, sellerRecipient);
        // Email 是 Member 模組提供的登入識別／聯絡地址；先獨立確認地址存在，
        // 再判斷 nullable 訂閱偏好，兩者不可互相替代。
        if (member == null || member.getEmail() == null || member.getEmail().isBlank()) {
            return null;
        }
        /*
         * 訂單通知：明確 false 才停用；其他模組未提供偏好（null）時視為已勾選。
         * 行銷通知：必須明確為 true；未提供或無法驗證時維持停用。
         */
        Boolean preference = type == NotificationType.ORDER
                ? member.getEmailOrderNotifications()
                : member.getEmailMarketingNotifications();
        boolean enabled = EmailSubscriptionPolicy.allows(type, preference);
        if (!enabled) {
            return null;
        }
        if (channelRepository.findByRecordIdAndChannelType(
                recordId, ChannelType.EMAIL).isPresent()) {
            throw new SysmsgConflictException("此 Record 已建立 EMAIL 渠道：" + recordId);
        }
        return channelRepository.save(new RecordChannelEntity(
                sendId, recordId, ChannelType.EMAIL, type));
    }

    @Override
    @Transactional
    public RecordChannelEntity markEmailSent(Integer id, String providerMessageId, LocalDateTime sentAt) {
        RecordChannelEntity channel = requireEmail(id);
        channel.markSent(providerMessageId, sentAt);
        return channelRepository.save(channel);
    }

    @Override
    @Transactional
    public RecordChannelEntity markEmailFailed(
            Integer id,
            String errorMessage,
            String failureCode,
            LocalDateTime attemptedAt,
            LocalDateTime nextRetryAt,
            int maxAttempts) {
        RecordChannelEntity channel = requireEmail(id);
        channel.markFailed(
                errorMessage,
                failureCode,
                attemptedAt,
                nextRetryAt,
                maxAttempts);
        return channelRepository.save(channel);
    }

    private RecordChannelEntity requireEmail(Integer id) {
        RecordChannelEntity channel = channelRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("找不到 RecordChannel：" + id));
        if (channel.getChannelType() != ChannelType.EMAIL) {
            throw new IllegalArgumentException("目前只支援 EMAIL Channel");
        }
        return channel;
    }
}
