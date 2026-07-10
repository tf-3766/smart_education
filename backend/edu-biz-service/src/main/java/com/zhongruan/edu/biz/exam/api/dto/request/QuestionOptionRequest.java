package com.zhongruan.edu.biz.exam.api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record QuestionOptionRequest(
        @NotBlank @Size(max = 16) String label,
        @NotBlank @Size(max = 10000) String content,
        @NotNull Boolean correct,
        @NotNull @Min(1) Integer sortOrder) {}
