package com.zhongruan.edu.biz.auth.api.vo;

import java.util.Set;

public record CurrentUserVO(
        String userId,
        String username,
        String displayName,
        String activeRole,
        Set<String> roles,
        Set<String> permissions) {
    public CurrentUserVO {
        roles = Set.copyOf(roles);
        permissions = Set.copyOf(permissions);
    }
}

