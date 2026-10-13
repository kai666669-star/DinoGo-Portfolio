package com.dinogo.member.dto;
import jakarta.validation.constraints.NotBlank;
public record DeactivateAccountRequest(@NotBlank String currentPassword) {}
