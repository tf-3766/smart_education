package com.zhongruan.edu.feign.ai;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** AI 自动流请求 biz 落库考试草稿（DRAFT，source=AI）。选题发卷仍由教师在现有 UI 完成。 */
public record AiExamDraftRequest(
        @NotNull Long userId,
        @NotNull String roleCode,
        @NotNull Long courseId,
        @NotNull String title,
        String description,
        Integer durationMinutes,
        @NotNull BigDecimal totalScore,
        String traceId) {}
