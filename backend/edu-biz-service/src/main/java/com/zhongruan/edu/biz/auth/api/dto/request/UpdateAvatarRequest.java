package com.zhongruan.edu.biz.auth.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateAvatarRequest(
        @Positive Long fileId,
        @NotNull @PositiveOrZero Integer version) {}
