package com.zhongruan.edu.feign.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AiAssistantContextRequest(
        @NotNull Long userId,
        @NotBlank String roleCode,
        @Size(max = 128) String traceId) {}