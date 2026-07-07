package com.zhongruan.edu.common.security;

import java.time.Instant;
import java.util.Set;

public record JwtClaims(
        Long userId,
        String username,
        String activeRole,
        Set<String> roles,
        Set<String> permissions,
        Instant issuedAt,
        Instant expiresAt) {
    public JwtClaims {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
        permissions = permissions == null ? Set.of() : Set.copyOf(permissions);
    }
}

