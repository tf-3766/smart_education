package com.zhongruan.edu.feign.ai;

import jakarta.validation.constraints.NotNull;

/** AI 自动流请求 biz 落库课程公告草稿（DRAFT，source=AI，不推送学生）。教师确认后才发布。 */
public record AiAnnouncementDraftRequest(
        @NotNull Long userId,
        @NotNull String roleCode,
        @NotNull Long courseId,
        @NotNull String title,
        @NotNull String content,
        String audience,
        String traceId) {}
