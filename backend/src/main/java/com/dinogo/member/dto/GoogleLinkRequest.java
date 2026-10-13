package com.dinogo.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLinkRequest(
        @NotBlank @Size(max = 10000) String credential,
        @NotBlank String password) {
}
