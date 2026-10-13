package com.dinogo.member.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record MemberStatusChangeRequest(@NotBlank @Size(max = 500) String reason) {}
