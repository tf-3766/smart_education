package com.zhongruan.edu.biz.auth.api.vo;

import java.time.Instant;
import java.util.Set;

public record LoginVO(
        String accessToken,
        String tokenType,
        long expiresIn,
        Instant expiresAt,
        CurrentUserVO user,
        Set<String> roles,
        Set<String> permissions) {
    public LoginVO {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }
}
