package com.zhongruan.edu.ai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CourseQaRequest(
        @NotBlank @Size(max = 2000) String question,
        @Positive Long lessonId) {}
