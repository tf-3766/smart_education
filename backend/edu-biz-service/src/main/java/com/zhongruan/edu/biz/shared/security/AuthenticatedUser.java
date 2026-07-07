package com.zhongruan.edu.biz.shared.security;

import java.security.Principal;
import java.util.Set;

public record AuthenticatedUser(
        Long userId,
        String username,
        String activeRole,
        Set<String> roles,
        Set<String> permissions) implements Principal {

    public AuthenticatedUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }

    @Override
    public String getName() {
        return username;
    }
}

