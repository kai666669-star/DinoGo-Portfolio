package com.dinogo.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = ".*[A-Za-z].*", message = "新密碼須包含英文") @Pattern(regexp = ".*\\d.*", message = "新密碼須包含數字") String newPassword,
        @NotBlank @Size(min = 8, max = 72) @Pattern(regexp = ".*[A-Za-z].*", message = "確認新密碼須包含英文") @Pattern(regexp = ".*\\d.*", message = "確認新密碼須包含數字") String confirmNewPassword) {
}
