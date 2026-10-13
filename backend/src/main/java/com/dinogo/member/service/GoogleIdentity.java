package com.dinogo.member.service;

public record GoogleIdentity(
        String subject,
        String email,
        String firstName,
        String lastName) {
}
