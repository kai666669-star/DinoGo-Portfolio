package com.dinogo.member.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = ".*[A-Za-z].*", message = "密碼須包含英文") @Pattern(regexp = ".*\\d.*", message = "密碼須包含數字") String password,
        @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = ".*[A-Za-z].*", message = "確認密碼須包含英文") @Pattern(regexp = ".*\\d.*", message = "確認密碼須包含數字") String confirmPassword,
        @NotBlank @Size(max = 50) String lastName,
        @NotBlank @Size(max = 50) String firstName,
        LocalDate birthDate,
        @Size(max = 20) String phone
) {
}
