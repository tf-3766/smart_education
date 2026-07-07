package com.zhongruan.edu.biz.course.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateChapterRequest(
        @NotBlank @Size(max = 120) String title,
        @Size(max = 2000) String description,
        @NotNull @Min(0) @Max(100000) Integer sortOrder) {}
