package com.zhongruan.edu.biz.platform.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateCourseCategoryRequest(
        @NotBlank @Size(max = 128) String name,
        @NotNull @Min(0) Integer sortOrder,
        @NotNull Boolean enabled,
        @NotNull @PositiveOrZero Integer version) {}
