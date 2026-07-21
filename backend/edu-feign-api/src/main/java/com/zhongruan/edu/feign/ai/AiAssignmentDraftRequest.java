package com.zhongruan.edu.feign.ai;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** AI 自动流请求 biz 落库作业草稿（TEXT 作答模式，source=AI，状态 DRAFT）。教师用既有发布流确认。 */
public record AiAssignmentDraftRequest(
        @NotNull Long userId,
        @NotNull String roleCode,
        @NotNull Long courseId,
        Long lessonId,
        @NotNull String title,
        String description,
        @NotNull BigDecimal maxScore,
        Integer dueInDays,
        String traceId) {}
