package com.dinogo.member.dto;

import java.util.List;

public record LoginResponse(
        String token,
        MemberResponse member,
        List<String> roles,
        Integer sellerId
) {

    public LoginResponse {
        roles = List.copyOf(roles);
    }
}
