package com.zhongruan.edu.feign.ai;

import jakarta.validation.constraints.NotNull;

public record AiResourceContextRequest(
        @NotNull Long userId,
        @NotNull String roleCode,
        @NotNull Long resourceId,
        @NotNull AiContextPurpose purpose,
        String traceId) {}
