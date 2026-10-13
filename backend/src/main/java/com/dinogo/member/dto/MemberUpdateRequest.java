package com.dinogo.member.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MemberUpdateRequest(
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Size(max = 50) String firstName,
        LocalDate birthDate,
        @Size(max = 20) String phone,
        Boolean emailOrderNotifications,
        Boolean emailMarketingNotifications
) {
    public MemberUpdateRequest(
            String lastName,
            String firstName,
            LocalDate birthDate,
            String phone) {
        this(lastName, firstName, birthDate, phone, null, null);
    }
}
