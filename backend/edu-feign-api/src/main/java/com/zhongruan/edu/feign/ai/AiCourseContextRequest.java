package com.zhongruan.edu.feign.ai;

import jakarta.validation.constraints.NotNull;

public record AiCourseContextRequest(
        @NotNull Long userId,
        @NotNull String roleCode,
        @NotNull Long courseId,
        Long lessonId,
        Long materialId,
        @NotNull AiContextPurpose purpose,
        String traceId) {}
