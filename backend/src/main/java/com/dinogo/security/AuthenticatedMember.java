package com.dinogo.security;

import java.security.Principal;

public record AuthenticatedMember(Integer memberId, String email) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
