package com.zhongruan.edu.biz.forum.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ForumVisibilityRequest(
        @NotNull Boolean visible,
        @Size(max = 300) String reason,
        @NotNull @Min(0) Integer version) {}
