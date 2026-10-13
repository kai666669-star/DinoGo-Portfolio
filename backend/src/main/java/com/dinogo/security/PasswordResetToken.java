package com.dinogo.security;

public record PasswordResetToken(Integer memberId, String email, int authVersion) {
}
