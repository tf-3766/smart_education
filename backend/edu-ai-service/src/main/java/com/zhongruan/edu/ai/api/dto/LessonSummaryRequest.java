package com.zhongruan.edu.ai.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LessonSummaryRequest(@NotNull @Positive Long courseId) {}
