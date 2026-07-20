package com.zhongruan.edu.biz.forum.api.dto.request;

import jakarta.validation.constraints.NotNull;

public record ForumPinRequest(
        @NotNull Boolean pinned,
        @NotNull Integer version) {}