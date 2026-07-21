package com.zhongruan.edu.feign.ai;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/** AI 自动流请求 biz 落库题库草稿（source=AI）。身份以 userId+roleCode 声明，biz 侧与 JWT 二次核对。 */
public record AiQuestionBankDraftRequest(
        @NotNull Long userId,
        @NotNull String roleCode,
        @NotNull Long courseId,
        @NotNull String bankName,
        String description,
        @NotEmpty List<AiQuestionDraft> questions,
        String traceId) {}
