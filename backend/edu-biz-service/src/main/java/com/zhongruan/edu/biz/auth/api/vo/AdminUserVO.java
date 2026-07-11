package com.zhongruan.edu.biz.auth.api.vo;

import java.time.OffsetDateTime;
import java.util.Set;

public record AdminUserVO(
        String userId,
        String username,
        String displayName,
        String userStatus,
        Set<String> roles,
        boolean superAdministrator,
        OffsetDateTime createdAt,
        Integer version) {
    public AdminUserVO {
        roles = Set.copyOf(roles);
    }
}
