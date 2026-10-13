package com.dinogo.member.service;

import java.nio.charset.StandardCharsets;

/** Shared password requirements for registration, reset, and password changes. */
final class PasswordPolicy {

    private PasswordPolicy() {
    }

    static void validate(String password, String fieldName) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException(fieldName + "不可超過 72 個 UTF-8 位元組");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalArgumentException(fieldName + "須包含英文與數字");
        }
    }
}
