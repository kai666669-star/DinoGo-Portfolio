package com.dinogo.member.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.dinogo.member.entity.Member;

public record MemberResponse(
        Integer memberId,
        String email,
        String lastName,
        String firstName,
        LocalDate birthDate,
        String phone,
        boolean emailOrderNotifications,
        boolean emailMarketingNotifications,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    // 保留既有測試與呼叫端的建構方式，時間欄位由 Member 實體映射時提供。
    public MemberResponse(
            Integer memberId,
            String email,
            String lastName,
            String firstName,
            LocalDate birthDate,
            String phone,
            String status) {
        this(memberId, email, lastName, firstName, birthDate, phone, true, false, status, null, null);
    }

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getMemberId(),
                member.getEmail(),
                member.getLastName(),
                member.getFirstName(),
                member.getBirthDate(),
                member.getPhone(),
                member.isEmailOrderNotifications(),
                member.isEmailMarketingNotifications(),
                member.getStatus(),
                member.getCreatedAt(),
                member.getUpdatedAt()
        );
    }
}
