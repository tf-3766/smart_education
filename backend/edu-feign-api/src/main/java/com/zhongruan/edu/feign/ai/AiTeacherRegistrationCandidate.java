package com.zhongruan.edu.feign.ai;

import java.time.OffsetDateTime;

/** Typed, role-scoped teacher registration candidate supplied by Biz to the AI service. */
public record AiTeacherRegistrationCandidate(
        Long userId,
        String username,
        String displayName,
        Integer version,
        OffsetDateTime createdAt) {}
