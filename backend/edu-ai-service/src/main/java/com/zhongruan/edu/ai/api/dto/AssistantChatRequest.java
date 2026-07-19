package com.zhongruan.edu.ai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AssistantChatRequest(
        @NotBlank @Size(max = 2000) String question,
        @Positive Long courseId,
        @Positive Long lessonId,
        @Size(max = 240) String pagePath,
        @Size(max = 120) String pageTitle,
        @Size(max = 64)
        @Pattern(regexp = "^[A-Za-z0-9._:-]+$", message = "conversationId 格式不正确")
        String conversationId) {}