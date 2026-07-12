package com.zhongruan.edu.feign.ai;

import jakarta.validation.constraints.NotNull;

public record AiPaperContextRequest(
        @NotNull Long userId,
        @NotNull String roleCode,
        @NotNull Long courseId,
        @NotNull AiContextPurpose purpose,
        String traceId) {}
